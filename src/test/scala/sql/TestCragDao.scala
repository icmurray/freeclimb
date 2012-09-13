package freeclimb.test.sql

import scala.actors.Futures.future

import java.sql.DriverManager

import org.scalatest.{FunSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import anorm._
import anorm.SqlParser._

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
  private def run[M[+_], A](action: ActionT[M, A, TransactionRepeatableRead]) = {
    action.runInTransaction(newSession())
  }

  describe("Crag DAO") {

    describe("The get action") {

      it("should return the latest revision of a crag that exists") {

        // Create a new Crag, and update it.
        val newCrag = run(cragDao.create(burbage)).toOption.get
        val update = Revisioned[Crag](newCrag.revision, Crag.makeUnsafe("burbage", "BURBAGE"))
        val updatedCrag = run(cragDao.update(update)).toOption.get

        // Get the Crag, and check the results
        val latestCrag = run(cragDao.get("burbage")).toOption.get.get
        latestCrag.revision should equal (updatedCrag.revision)
        latestCrag.model should equal (updatedCrag.model)
      }

      it("should return None if the crag does not exist") {
        val someCrag = run(cragDao.get("burbage")).toOption.get
        someCrag should equal (None)
      }

    }

    describe("The create action") {
      
      it("should successfuly create a new crag") {

        val result = run(cragDao.create(burbage))

        result.fold (
          error => fail ("Failed to create new Crag: " + error.toString),
          revision => {
            // Check the returned Revisioned[Crag]
            revision.model should equal (burbage)

            // Check the Crag was stored in the database
            val someStoredCrag = run(cragDao.get("burbage")).toOption.get
            someStoredCrag match {
              case None             => fail ("Failed to obtain stored Crag.")
              case Some(storedCrag) => {
                storedCrag.model should equal (burbage)
                storedCrag.revision should equal (revision.revision)
              }
            }
          }
        )
      }

      it("should not create a crag if it already exists") {

        // Create the Crag initially.
        run(cragDao.create(burbage))

        // Now try to re-create it
        val result = run(cragDao.create(burbage))

        result.fold (
          error => {},
          revision => fail ("Crag was re-created")
        )
      }

      it("should return a concurrent update if crags with the same name are created at the same time") {

        // First, start a transaction that creates a new Crag, but don't complete it.
        val session1 = newSession()
        session1.dbConnection.setAutoCommit(false)
        session1.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        val newCrag = cragDao.create(burbage).runWith(session1)
        newCrag.fold (
          error => { session1.dbConnection.close() ; fail ("Couldn't create crag") },
          success => success.model should equal (burbage)
        )

        // Now, create a second session to create the same Crag concurrently.
        val session2 = newSession()
        session2.dbConnection.setAutoCommit(false)
        session2.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)

        // The new Crag is created within a new Thread
        val f = future {
          cragDao.create(burbage).runInTransaction(session2)
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
        val firstRevision = run(cragDao.create(burbage)).toOption.get

        val updatedCrag = Crag.makeUnsafe("burbage", "BURBAGE")
        val rev: Revisioned[Crag] = Revisioned[Crag](firstRevision.revision, updatedCrag)
        val result = run(cragDao.update(rev))

        result.fold (
          error => fail ("Failed to update Crag: " + error.toString),
          revision => {
            // Check the returned Revisioned[Crag]
            revision.model should equal (updatedCrag)
            revision.revision should be > (firstRevision.revision)

            // Check the update was stored in the database
            val someStoredCrag = run(cragDao.get("burbage")).toOption.get
            someStoredCrag match {
              case None             => fail ("Failed to obtain stored Crag.")
              case Some(storedCrag) => {
                storedCrag.model should equal (updatedCrag)
                storedCrag.revision should equal (revision.revision)
              }
            }
          }
        )
      }

      it("should inform if the crag has been updated concurrently") {

        // First, create a Crag to update
        val newCrag = run(cragDao.create(burbage)).toOption.get

        // Now, try to update it with a smaller revision number
        val result = run(cragDao.update(Revisioned[Crag](newCrag.revision-1, newCrag.model)))
        result fold (
          error       => {},
          updatedCrag => fail ("Update should have failed: " + updatedCrag)
        )
      }

      it("should inform if the crag is being updated concurrently in an other transaction") {

        // Create a new Crag to work upon
        val newCrag = run(cragDao.create(burbage)).toOption.get

        // First, start a transaction that updates the new Crag, but don't complete it.
        val update1 = Revisioned[Crag](newCrag.revision, Crag.makeUnsafe("burbage", "BURBAGE"))
        val session1 = newSession()
        session1.dbConnection.setAutoCommit(false)
        session1.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        val updatedCrag = cragDao.update(update1).runWith(session1)
        updatedCrag.fold (
          error => { session1.dbConnection.close() ; fail ("Couldn't update crag") },
          success => success.model should equal (update1.model)
        )

        // Now, create a second session to update the same Crag concurrently.
        val session2 = newSession()
        session2.dbConnection.setAutoCommit(false)
        session2.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)

        // The new Crag is updated within a new Thread
        val update2 = Revisioned[Crag](newCrag.revision, Crag.makeUnsafe("burbage", "BURBAGE BURBAGE"))
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

    }

    describe("The delete action") {

      it("should delete an existing crag successfully") {

        val action = for {
          val cragRev  <- cragDao.create(burbage)
          val _ = cragRev.model should equal (burbage)
          val _        <- cragDao.delete(cragRev)
        } yield ()

        run(action)
        val someCrag = run(cragDao.get("burbage")).toOption.get
        someCrag should equal (None)
      }

      it("should be possible to create a new crag with the same name of a previsouly deleted crag") {
        val action = for {
          val rev <- cragDao.create(burbage)
          val _ <- cragDao.delete(rev)
          val newRev <- cragDao.create(burbage)
        } yield newRev

        val newCrag = run(action)
        newCrag fold (
          error   => fail("Couldn't re-create deleted crag"),
          cragRev => {
            cragRev.model should equal (burbage)
          }
        )
      }

      it("should inform if the crag has been updated concurrently") {
        val cragRev = run(cragDao.create(burbage)).toOption.get

        val result = run(cragDao.delete(Revisioned[Crag](cragRev.revision-1, cragRev.model)))
        result fold (
          error       => {},
          deletedCrag => fail("Delete should have failed")
        )
      }

      it("should inform if the crag has been deleted concurrently") {
        val cragRev = run(cragDao.create(burbage)).toOption.get
        val _ = run(cragDao.delete(cragRev))

        val result = run(cragDao.delete(Revisioned[Crag](cragRev.revision, cragRev.model)))
        result fold (
          error       => {},
          deletedCrag => fail("Delete should have failed")
        )
      }

    }

    describe("the history action") {

      it("should return an existing Crag's history of edits") {
        // Create a Crag, and update it a few times.
        val rev1 = run(cragDao.create(burbage)).toOption.get
        val rev2 = run(cragDao.update(Revisioned[Crag](rev1.revision,
                                                       Crag.makeUnsafe("burbage", "BURBAGE")))).toOption.get
        val rev3 = run(cragDao.update(Revisioned[Crag](rev2.revision,
                                                       Crag.makeUnsafe("burbage", "BURBAGE !!")))).toOption.get

        val history = run(cragDao.history(burbage)).toOption.get
        history.toList should equal (List(rev3, rev2, rev1))
      }

      it("should return the empty list for a Crag that does not exist") {
        val history = run(cragDao.history(burbage)).toOption.get
        history.toList should equal (Nil)
      }

      it("should return the empty list for a Crag that has been deleted") {
        run(for {
          val rev <- cragDao.create(burbage)
          val _   <- cragDao.delete(rev)
        } yield ())

        val history = run(cragDao.history(burbage)).toOption.get
        history.toList should equal (Nil)
      }

      it("should not return old history for a new Crag that overwrites a previous Crag") {
        // Create and delete a Crag
        val rev = run(for{
          val rev <- cragDao.create(burbage)
          val deleted <- cragDao.delete(rev)
        } yield deleted)

        // Create a new Crag with the same name
        val newCrag = Crag.makeUnsafe("burbage", "BURBAGE")
        val newRev = run(cragDao.create(newCrag)).toOption.get

        val history = run(cragDao.history(burbage)).toOption.get

        history.toList should equal (List(newRev))

      }

    }

    describe("the purge action") {

      it("should remove the Crag and it's history") {

        val action = for {
          val cragRev  <- cragDao.create(burbage)
          val _ = cragRev.model should equal (burbage)
          val _        <- cragDao.purge(cragRev)
        } yield ()

        run(action)
        val someCrag = run(cragDao.get("burbage")).toOption.get
        someCrag should equal (None)

      }

      it("should be possible to create a new crag with the same name of a previsouly purged crag") {
        val action = for {
          val rev <- cragDao.create(burbage)
          val _ <- cragDao.purge(rev)
          val newRev <- cragDao.create(burbage)
        } yield newRev

        val newCrag = run(action)
        newCrag fold (
          error   => fail("Couldn't re-create purged crag"),
          cragRev => {
            cragRev.model should equal (burbage)
          }
        )
      }
      
      it("should inform if the crag has been updated concurrently") {
        val cragRev = run(cragDao.create(burbage)).toOption.get

        val result = run(cragDao.purge(Revisioned[Crag](cragRev.revision-1, cragRev.model)))
        result fold (
          error       => {},
          deletedCrag => fail("Purge should have failed")
        )
      }

      it("should inform if the crag has been purged concurrently") {
        val cragRev = run(cragDao.create(burbage)).toOption.get
        val _ = run(cragDao.purge(cragRev))

        val result = run(cragDao.purge(Revisioned[Crag](cragRev.revision, cragRev.model)))
        result fold (
          error       => {},
          deletedCrag => fail("Delete should have failed")
        )
      }
    }

  }
  
  private val burbage = Crag.makeUnsafe("burbage", "Burbage")
}
