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

  describe("Crag DAO") {

    describe("The get action") {

      it("should return the latest revision of a crag that exists") {
        // Create a new Crag, and update it.
        var session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val createAndUpdate = for {
          val newCrag <- cragDao.create(burbage)
          val newRevision = Revisioned[Crag](newCrag.revision, Crag.makeUnsafe("burbage", "BURBAGE"))
          val updatedCrag <- cragDao.update(newRevision)
        } yield updatedCrag
        val updatedCrag: Revisioned[Crag] = createAndUpdate.runInTransaction(session).toOption.get

        // Get the Crag, and check the results
        session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val latestCrag = cragDao.get("burbage").runInTransaction(session).get
        latestCrag.revision should equal (updatedCrag.revision)
        latestCrag.model should equal (Crag.makeUnsafe("burbage", "BURBAGE"))
      }

      it("should return None if the crag does not exist") {
        var session = TestDatabaseSessions.newSession()
        val someCrag = cragDao.get("burbage").runInTransaction(session)
        someCrag should equal (None)
      }

    }

    describe("The create action") {
      
      it("should successfuly create a new crag") {

        val session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val result = cragDao.create(burbage).runInTransaction(session)

        result.fold (
          error => fail ("Failed to create new Crag: " + error.toString),
          revision => {
            // Check the returned Revisioned[Crag]
            revision.model should equal (burbage)

            // Check the Crag was stored in the database
            val newSession = TestDatabaseSessions.newSession()
            val someStoredCrag = cragDao.get("burbage").runInTransaction(newSession)
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
        var session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        cragDao.create(burbage).runInTransaction(session)

        // Now try to re-create it
        session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val result = cragDao.create(burbage).runInTransaction(session)

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
        var session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val firstRevision = cragDao.create(burbage).runInTransaction(session).toOption.get

        val updatedCrag = Crag.makeUnsafe("burbage", "BURBAGE")
        session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val rev: Revisioned[Crag] = Revisioned[Crag](firstRevision.revision, updatedCrag)
        val result = cragDao.update(rev).runInTransaction(session)

        result.fold (
          error => fail ("Failed to update Crag: " + error.toString),
          revision => {
            // Check the returned Revisioned[Crag]
            revision.model should equal (updatedCrag)
            revision.revision should be > (firstRevision.revision)

            // Check the update was stored in the database
            session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
            val someStoredCrag = cragDao.get("burbage").runInTransaction(session)
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
        var session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val newCrag = cragDao.create(burbage).runInTransaction(session).toOption.get

        // Now, try to update it with a smaller revision number
        session = TestDatabaseSessions.newSession(TransactionRepeatableRead)
        val result = cragDao.update(Revisioned[Crag](newCrag.revision-1, newCrag.model)).
                             runInTransaction(session)
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
