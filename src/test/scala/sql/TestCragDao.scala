package freeclimb.test.sql

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
        val latestCrag = run(cragDao.get("burbage")).get
        latestCrag.revision should equal (updatedCrag.revision)
        latestCrag.model should equal (updatedCrag.model)
      }

      it("should return None if the crag does not exist") {
        val someCrag = run(cragDao.get("burbage"))
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
            val someStoredCrag = run(cragDao.get("burbage"))
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

      it("should return a concurrent update if crags with the same name are created at the same time") (pending)

        //// The first transaction
        //println("Before first transaction")
        //val firstSession = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        //firstSession.dbConnection.setAutoCommit(false)
        //firstSession.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        //cragDao.create(burbage).runWith(firstSession)

        //// Now, a second transaction, whilsts the first is still running
        //println("Before second transaction")
        //val secondSession = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        //println("Before autocommit")
        //secondSession.dbConnection.setAutoCommit(false)
        //println("Before setting level")
        //secondSession.dbConnection.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
        //println("Before runWith")
        //cragDao.create(burbage).runWith(secondSession)
        //println ("Before both commits")
        //secondSession.dbConnection.commit()
        //firstSession.dbConnection.commit()

      //}
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
            val someStoredCrag = run(cragDao.get("burbage"))
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

    }

    describe("The delete action") {

      it("should delete an existing crag successfully") (pending)

      it("should be possible to undelete a deleted crag") (pending)

      it("should inform if the crag has been deleted concurrently") (pending)

      it("should inform if the crag has been edited concurrently") (pending)

    }
  }
  
  private val burbage = Crag.makeUnsafe("burbage", "Burbage")
}
