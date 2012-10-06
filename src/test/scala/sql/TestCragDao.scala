package freeclimb.test.sql

import scala.actors.Futures.future

import java.sql.DriverManager

import org.scalatest.{FunSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import anorm._
import anorm.SqlParser._

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

class CragDaoTest extends FunSpec
                     with BeforeAndAfter
                     with ShouldMatchers {

  val cragDao: CragDao = CragDao

  private def cleanTables() {
    implicit val connection = TestDatabaseSessions.newSession(TransactionRepeatableRead).
                                                   dbConnection
    SQL("DELETE FROM crags;").execute()
    SQL("DELETE FROM crag_history;").execute()
    connection.close()
  }

  before {
    cleanTables()
  }

  after {
    cleanTables()
  }

  private def newSession() = TestDatabaseSessions.newSession(TransactionRepeatableRead)
  private def run[M[+_], A](action: ApiAction[A, TransactionRepeatableRead]) = {
    action.runInTransaction(newSession())
  }

  describe("Crag DAO") {

    describe("The get action") {

      it("should return the latest revision of a crag that exists") {

        // Create a new Crag, and update it.
        val updatedCrag = run {
            for {
              newCrag <- cragDao.create(burbage)
              val update = Revisioned[Crag](newCrag.revision, newBurbage)
              updatedCrag <- cragDao.update(update)
            } yield updatedCrag
        } getOrElse fail("Unabled to create test fixture")

        // Get the Crag, and check the results
        val latestCrag = run {
          cragDao.get("burbage")
        } getOrElse fail("No crag found.")

        latestCrag.revision should equal (updatedCrag.revision)
        latestCrag.model should equal (updatedCrag.model)
      }

      it("should return NotFound if the crag does not exist") {
        val someCrag = run {
          cragDao.get("burbage")
        }.swap getOrElse fail("Managed to find non-existant Crag!")

        someCrag should equal (NotFound())
      }

    }

    describe("The getOption action") {

      it("should return the latest revision of a crag that exists") {

        // Create a new Crag, and update it.
        val updatedCrag = run {
            for {
              newCrag <- cragDao.create(burbage)
              val update = Revisioned[Crag](newCrag.revision, newBurbage)
              updatedCrag <- cragDao.update(update)
            } yield updatedCrag
        } getOrElse fail("Unabled to create test fixture")

        // Get the Crag, and check the results
        val latestCrag = run {
          cragDao.getOption("burbage")
        } getOrElse fail("Error finding Crag")

        latestCrag match {
          case None             => fail("No crag found")
          case Some(latestCrag) => {
            latestCrag.revision should equal (updatedCrag.revision)
            latestCrag.model should equal (updatedCrag.model)
          }
        }
      }

      it("should return None if the crag does not exist") {
        val someCrag = run {
          cragDao.getOption("burbage")
        } getOrElse fail("Error finding Crag")

        someCrag should equal (None)
      }

    }
    describe("The create action") {
      
      it("should successfuly create a new crag") {

        val revision = run{
          cragDao.create(burbage)
        } getOrElse fail("Failed to create new Crag")

        // Check the returned Revisioned[Crag]
        revision.model should equal (burbage)

        // Check the Crag was stored in the database
        val storedCrag = run {
          cragDao.get("burbage")
        } getOrElse fail("Failed to obtain stored Crag")

        storedCrag.model should equal (burbage)
        storedCrag.revision should equal (revision.revision)
      }

      it("should not create a crag if it already exists") {

        // Create the Crag initially.
        run { cragDao.create(burbage) }

        // Now try to re-create it
        run {
          cragDao.create(burbage)
        }.swap getOrElse fail("Crag was re-created")

      }

      it("should return a concurrent update if crags with the same name are created at the same time") {

        // First, start a transaction that creates a new Crag, but don't complete it.
        val session1 = newSession()
        session1.dbConnection.setAutoCommit(false)
        session1.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        val newCrag = cragDao.create(burbage).runWith(session1)
        newCrag.fold (
          error => { session1.dbConnection.close() ; fail ("Couldn't create crag") },
          success => success._2.model should equal (burbage)
        )

        // Now, create a second session to create the same Crag concurrently.
        val session2 = newSession()
        session2.dbConnection.setAutoCommit(false)
        session2.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)

        // The new Crag is created within a new Thread
        val f = future {
          cragDao.create(newBurbage).runInTransaction(session2)
        }

        // *try* to ensure the second transaction has started concurrently
        // by pausing this Thread, giving the future time to execute.
        Thread.sleep(100) 

        // Finally, complete the first session.
        session1.dbConnection.commit()

        // We expect the second session to have failed.
        f() fold (
          error   => error should equal (EditConflict()),
          success => fail ("Concurrent update was not raised")
        )
      }
    }

    describe("The update action") {

      it("should update an existing crag successfully") {
        // Create the Crag we're ging to update.
        val firstRevision = run {
          cragDao.create(burbage)
        } getOrElse fail("Could not create fixture")

        val rev = Revisioned[Crag](firstRevision.revision, newBurbage)
        val revision = run {
          cragDao.update(rev)
        } getOrElse fail("Failed to update Crag")

        // Check the returned Revisioned[Crag]
        revision.model should equal (newBurbage)
        revision.revision should be > (firstRevision.revision)

        // Check the update was stored in the database
        val storedCrag = run {
          cragDao.get("burbage")
        } getOrElse fail("Failed to obtain stored Crag")

        storedCrag.model should equal (newBurbage)
        storedCrag.revision should equal (revision.revision)
      }

      it("should inform if the crag has been updated concurrently") {

        // First, create a Crag to update
        val newCrag = run {
          cragDao.create(burbage)
        } getOrElse fail("Failed to create Crag")

        // Now, try to update it with a smaller revision number
        val result = run {
          cragDao.update(Revisioned[Crag](newCrag.revision-1, newBurbage))
        }.swap getOrElse fail("Updated should have failed.")
      }

      it("should inform if the crag is being updated concurrently in an other transaction") {

        // Create a new Crag to work upon
        val newCrag = run {
          cragDao.create(burbage)
        } getOrElse fail("Failed to create new Crag")

        // First, start a transaction that updates the new Crag, but don't complete it.
        val update1 = Revisioned[Crag](newCrag.revision, newBurbage)
        val session1 = newSession()
        session1.dbConnection.setAutoCommit(false)
        session1.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        val updatedCrag = cragDao.update(update1).runWith(session1)
        updatedCrag.fold (
          error => { session1.dbConnection.close() ; fail ("Couldn't update crag") },
          success => success._2.model should equal (update1.model)
        )

        // Now, create a second session to update the same Crag concurrently.
        val session2 = newSession()
        session2.dbConnection.setAutoCommit(false)
        session2.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)

        // The new Crag is updated within a new Thread
        val update2 = Revisioned[Crag](newCrag.revision, newestBurbage)
        val f = future {
          cragDao.update(update2).runInTransaction(session2)
        }

        // *try* to ensure the second transaction has started concurrently
        // by pausing this Thread, giving the future time to execute.
        Thread.sleep(100) 

        // Finally, complete the first session.
        session1.dbConnection.commit()

        // We expect the second session to have failed.
        f() fold (
          error   => error should equal (EditConflict()),
          success => fail ("Concurrent update was not raised")
        )
      }

      it("should not record updates that don't change anything") {
        // Create the Crag we're ging to update.
        val firstRevision = run {
          cragDao.create(burbage)
        } getOrElse fail("Could not create fixture")

        val rev = Revisioned[Crag](firstRevision.revision, burbage)
        val revision = run {
          cragDao.update(rev)
        } getOrElse fail("Failed to update Crag")

        // Check the returned Revisioned[Crag]
        revision.model should equal (burbage)
        revision.revision should be > (firstRevision.revision)

        // Check the update was stored in the database
        val storedCrag = run {
          cragDao.get("burbage")
        } getOrElse fail("Failed to obtain stored Crag")

        storedCrag.model should equal (burbage)
        storedCrag.revision should equal (revision.revision)

        // Check the history contains only one entry, but with
        // the latest revision.
        val history = run {
          cragDao.history(burbage)
        } getOrElse fail("Failed to get history")

        history.toList.length should equal (1)
        history.toList.head.model should equal (burbage)
        history.toList.head.revision should equal (revision.revision)
        
      }

    }

    describe("The delete action") {

      it("should delete an existing crag successfully") {

        val rev = run {
          for {
            val cragRev  <- cragDao.create(burbage)
            val _ = cragRev.model should equal (burbage)
            val _        <- cragDao.delete(cragRev)
          } yield cragRev
        } getOrElse fail("Failed to create test Crag")

        val someCrag = run {
          cragDao.get("burbage")
        }.swap getOrElse fail("Found deleted Crag!")

        someCrag should equal (NotFound())
        
        val deletedCrags = run {
          cragDao.deletedList()
        } getOrElse fail("Couldn't retrieve deleted Crags")

        deletedCrags.toList should contain (rev)
      }

      it("should be possible to create a new crag with the same name of a previsouly deleted crag") {
        val (origRev, newCrag) = run {
          for {
            val rev <- cragDao.create(burbage)
            val _ <- cragDao.delete(rev)
            val newRev <- cragDao.create(burbage)
          } yield (rev, newRev)
        } getOrElse fail ("Couldn't re-create deleted crag")

        newCrag.model should equal (burbage)

        val deletedCrags = run {
          cragDao.deletedList()
        } getOrElse fail("Couldn't retrieve deleted Crags")

        deletedCrags.toList should contain (origRev)
      }

      it("should inform if the crag has been updated concurrently") {
        val cragRev = run {
          cragDao.create(burbage)
        } getOrElse fail("Could not create Crag")

        val result = run {
          cragDao.delete(Revisioned[Crag](cragRev.revision-1, newBurbage))
        }.swap getOrElse fail("Delete should have failed")
      }

      it("should inform if the crag has been deleted concurrently") {
        val cragRev = run {
          for {
            cragRev <- cragDao.create(burbage)
            _       <- cragDao.delete(cragRev)
          } yield cragRev
        } getOrElse fail("Could not create new Crag")

        run {
          cragDao.delete(cragRev)
        }.swap getOrElse fail("Delete should have failed")
      }

      it("should fail if the crag has climbs associated with it") {
        val result = run {
          for {
            rev1 <- cragDao.create(burbage)
            _    <- ClimbDao.create(harvest)
            rev2 <- cragDao.get("burbage")
            res  <- cragDao.delete(rev2)
          } yield res
        }.swap getOrElse fail("Deleted crag with climbs associated")

        result should equal (ValidationError())
      }

    }

    describe("the history action") {

      it("should return an existing Crag's history of edits") {

        // Create a Crag, and update it a few times.
        val (rev1, rev2, rev3) = run {
          for {
            rev1 <- cragDao.create(burbage)
            rev2 <- cragDao.update(Revisioned[Crag](rev1.revision, newBurbage))
            rev3 <- cragDao.update(Revisioned[Crag](rev2.revision, newestBurbage))
          } yield (rev1, rev2, rev3)
        } getOrElse fail("Couldn't create fixture Crag")

        val history = run {
          cragDao.history(burbage)
        } getOrElse fail("Couldn't retrieve history")

        history.toList should equal (List(rev3, rev2, rev1))

      }

      it("should return the empty list for a Crag that does not exist") {
        val history = run {
          cragDao.history(burbage)
        } getOrElse fail("Couldn't retrieve history")

        history.toList should equal (Nil)
      }

      it("should return the empty list for a Crag that has been deleted") {
        run {
          for {
            val rev <- cragDao.create(burbage)
            val _   <- cragDao.delete(rev)
          } yield ()
        }

        val history = run {
          cragDao.history(burbage)
        } getOrElse fail("Couldn't retrieve history")
        
        history.toList should equal (Nil)
      }

      it("should not return old history for a new Crag that overwrites a previous Crag") {
        // Create and delete a Crag
        val rev = run {
          for {
            val rev <- cragDao.create(burbage)
            val deleted <- cragDao.delete(rev)
          } yield deleted
        }

        // Create a new Crag with the same name
        val newRev = run {
          cragDao.create(newBurbage)
        } getOrElse fail("Couldn't create Crag")

        val history = run {
          cragDao.history(burbage)
        } getOrElse fail("Couldn't retrieve history")

        history.toList should equal (List(newRev))

      }

    }

    describe("the deletedList action") {
      it("should be empty if Crags have not been deleted") {
        // Create a Crag, and update it.  But don't delete it.
        run {
          for {
            rev <- cragDao.create(burbage)
            _   <- cragDao.update(Revisioned[Crag](rev.revision, newBurbage))
          } yield ()
        } getOrElse fail("Error setting up fixture")

        // Check deleted list is empty
        val deletedList = run {
          cragDao.deletedList()
        } getOrElse fail("Error retrieving deleted list")

        deletedList.toList should equal (Nil)

      }
      
      it("should contain only the latest revision of a deleted Crag") {
        // Create a Crag, and update it, then delete it.
        val (rev1, rev2) = run {
          for {
            rev1 <- cragDao.create(burbage)
            rev2 <- cragDao.update(Revisioned[Crag](rev1.revision, newBurbage))
            _    <- cragDao.delete(rev2)
          } yield (rev1, rev2)
        } getOrElse fail ("Error setting up fixture")

        // Check the deleted list
        val deletedList = run {
          cragDao.deletedList()
        } getOrElse fail("Error retrieving deleted list")

        deletedList.toList should contain (rev2)
        deletedList.toList should not contain (rev1)
      }
    }

    describe("the purge action") {

      it("should remove the Crag and it's history") {

        run {
          for {
            val cragRev  <- cragDao.create(burbage)
            val _ = cragRev.model should equal (burbage)
            val _        <- cragDao.purge(cragRev)
          } yield ()
        }

        val someCrag = run {
          cragDao.get("burbage")
        }.swap getOrElse fail("Found purged Crag!")
        
        someCrag should equal (NotFound())

        val deletedCrags = run {
          cragDao.deletedList()
        } getOrElse fail("Failed to retrieve deleted Crags")

        deletedCrags should equal (Nil)
      }

      it("should be possible to create a new crag with the same name of a previsouly purged crag") {
        val newCrag = run {
          for {
            val rev <- cragDao.create(burbage)
            val _ <- cragDao.purge(rev)
            val newRev <- cragDao.create(burbage)
          } yield newRev
        } getOrElse fail("Couldn't re-create purged crag")

        newCrag.model should equal (burbage)
      }
      
      it("should inform if the crag has been updated concurrently") {
        val cragRev = run {
          cragDao.create(burbage)
        } getOrElse fail("Failed to create Crag")

        val result = run {
          cragDao.purge(Revisioned[Crag](cragRev.revision-1, cragRev.model))
        }.swap getOrElse fail("Purge should have failed")
        
        result should equal (EditConflict())
      }

      it("should inform if the crag has been purged concurrently") {
        val cragRev = run {
          for {
            cragRev <- cragDao.create(burbage)
            _       <- cragDao.purge(cragRev)
          } yield cragRev
        } getOrElse fail("Failed to create and purge Crag")

        val result = run {
          cragDao.purge(cragRev)
        }.swap getOrElse fail("Delete should have failed")
        
        result should equal (NotFound())
      }
      
      it("should fail if the crag has climbs associated with it") {
        val result = run {
          for {
            rev1 <- cragDao.create(burbage)
            _    <- ClimbDao.create(harvest)
            rev2 <- cragDao.get("burbage")
            res  <- cragDao.purge(rev2)
          } yield res
        }.swap getOrElse fail("Purged crag with climbs associated")

        result should equal (ValidationError())
      }
    }

  }
  
  private val burbage = Crag.makeUnsafe("burbage", "Burbage")
  private val newBurbage = Crag.makeUnsafe("burbage", "BURBAGE")
  private val newestBurbage = Crag.makeUnsafe("burbage", "BURBAGE BURBAGE")
  
  private val harvest = Climb.makeUnsafe(
    "harvest",
    "Harvest",
    "It is brutal, and on the wrong crag.",
    burbage,
    EuSport(Grade.EuSport.Eu7a))
}
