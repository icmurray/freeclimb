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

class ClimbDaoTest extends FunSpec
                   with BeforeAndAfter
                   with ShouldMatchers {

  val climbDao: ClimbDao = ClimbDao

  private def cleanTables() {
    implicit val connection = TestDatabaseSessions.newSession(TransactionRepeatableRead).
                                                   dbConnection
    SQL("DELETE FROM climbs;").execute()
    SQL("DELETE FROM climb_history;").execute()
    SQL("DELETE FROM crags;").execute()
    SQL("DELETE FROM crag_history;").execute()
    connection.close()
  }

  before {
    cleanTables()
  }

  after {
    //cleanTables()
  }

  private def newSession() = TestDatabaseSessions.newSession(TransactionRepeatableRead)
  private def run[M[+_], A](action: ApiAction[A, TransactionRepeatableRead]) = {
    action.runInTransaction(newSession())
  }

  describe("Climb DAO") {

    describe("The get action") {
      it("should return the latest version of a climb that exists") {
        // Create the crags
        run {
          CragDao.create(burbage)
        } getOrElse fail("Failed to create fixture crag")

        // Create a new Climb, and update it.
        val updatedClimb = run {
            for {
              newClimb <- climbDao.create(harvest)
              val update = Revisioned[Climb](newClimb.revision, newHarvest)
              updatedClimb <- climbDao.update(update)
            } yield updatedClimb
        } getOrElse fail("Unable to create test fixture")

        // Get the Climb, and check the results
        val latestClimb = run {
          climbDao.get("burbage", "harvest")
        } getOrElse fail("No climb found.")

        latestClimb.revision should equal (updatedClimb.revision)
        latestClimb.model.name should equal ("harvest")
        latestClimb.model should equal (updatedClimb.model)

      }
    }

    describe("The create action") {
      
      it("should successfuly create a new climb") {

        run {
          CragDao.create(burbage)
        } getOrElse fail("Failed to create fixture crag")

        val revision = run {
          climbDao.create(harvest)
        } getOrElse fail("Failed to create new Climb")

        // Check we can retrieve the newly created climb.
        val latestClimb = run {
          climbDao.getOption("burbage", "harvest")
        } getOrElse fail("No climb found")

      }

    }

  }
  
  private val burbage = Crag.makeUnsafe("burbage", "Burbage")
  private val stanage = Crag.makeUnsafe("stanage", "Stanage")

  private val harvest = Climb.makeUnsafe(
    "harvest",
    "Harvest",
    "It is brutal",
    burbage,
    EuSport(Grade.EuSport.Eu7a))

  private val newHarvest = Climb.makeUnsafe(
    "harvest",
    "HARVEST",
    "Still brutal",
    burbage,
    UkTrad(Grade.UkAdjective.E4, Grade.UkTechnical.T6a))

  private val newerHarvest = Climb.makeUnsafe(
    "harvest",
    "Harvest!!",
    "Did I mention it's brutal?",
    stanage,
    UkTrad(Grade.UkAdjective.E4, Grade.UkTechnical.T6a))



}

