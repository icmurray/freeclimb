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
    cleanTables()
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

      it("should return NotFound if the climb does not exist") {
        val someClimb = run {
          climbDao.get("burbage", "harvest")
        }.swap getOrElse fail("Managed to find non-existeant Climb!")

        someClimb should equal (NotFound())
      }

      it("should return NotFound if the climb's crag does not exist") {

        // create harvest at burbage
        run {
          for {
            _ <- CragDao.create(burbage)
            _ <- ClimbDao.create(harvest)
          } yield ()
        } getOrElse fail("Failed to create fixture climb")

        // and try to locate it at stanage
        val someClimb = run {
          climbDao.get("stanage", "harvest")
        }.swap getOrElse fail("Managed to find non-existeant Climb!")

        someClimb should equal (NotFound())
      }
    }

    describe("The getOption action") {
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
          climbDao.getOption("burbage", "harvest")
        } getOrElse fail("No climb found.")

        latestClimb should not equal (None)
        latestClimb.get.revision should equal (updatedClimb.revision)
        latestClimb.get.model should equal (updatedClimb.model)
      }

      it("should return None if the climb does not exist") {
        val someClimb = run {
          climbDao.getOption("burbage", "harvest")
        } getOrElse fail("Failed to run getOption")

        someClimb should equal (None)
      }

      it("should return None if the climb's crag does not exist") {

        // create harvest at burbage
        run {
          for {
            _ <- CragDao.create(burbage)
            _ <- ClimbDao.create(harvest)
          } yield ()
        } getOrElse fail("Failed to create fixture climb")

        // and try to locate it at stanage
        val someClimb = run {
          climbDao.getOption("stanage", "harvest")
        } getOrElse fail("Failed to run getOption")

        someClimb should equal (None)
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
      
      it("should not create a climb if it already exists") {

        run {
          CragDao.create(burbage)
        } getOrElse fail("Failed to create fixture crag")

        // Create the Crag initially.
        run { climbDao.create(harvest) } getOrElse fail ("Failed to create fixture")

        // Now try to re-create it
        run {
          climbDao.create(harvest)
        }.swap getOrElse fail("Climb was re-created")

      }

      it("should not create a climb if the crag does not exist") {
        val error = run {
          climbDao.create(harvest)
        }.swap getOrElse fail("Managed to create a climb without a crag!")

        error should equal (ValidationError())
      }

      it("should be possible to have two climbs with the same name on different crags") {
        run {
          for {
            b <- CragDao.create(burbage)
            s <- CragDao.create(stanage)
          } yield ()
        } getOrElse fail("Unable to create fixture crags")

        run { climbDao.create(harvest) } getOrElse fail("Unable to create fixture climb")

        run {
          climbDao.create(newerHarvest)
        } getOrElse fail("Couldn't create climb with same name on different crag")

      }

      it("should bump the revision number of the crag that the climb is created against") {
        val originalCragRev = run {
          CragDao.create(burbage)
        } getOrElse fail("Failed to create crag fixture")

        run {
          climbDao.create(harvest)
        } getOrElse fail("Couldn't create climb fixture")

        val newCragRev = run { CragDao.get("burbage") } getOrElse fail("Failed to retrieve crag")

        newCragRev.revision should be > (originalCragRev.revision)
      }

      it("should return a concurrent update if climbs with the same name are created at the same time") (pending)
    }

    describe("The update action") {
      it("should update an existing climb successfully") {
        val (first, second) = run {
          for {
            _ <- CragDao.create(burbage)
            first <- climbDao.create(harvest)
            second <- climbDao.update(Revisioned[Climb](first.revision, newHarvest))
          } yield (first, second)
        } getOrElse fail("Failed to create fixtures")

        first.revision should be < (second.revision)
        second.model should equal (newHarvest)
      }

      it("should inform if the climb has been updated concurrently") {
        val (first, second) = run {
          for {
            _ <- CragDao.create(burbage)
            first <- climbDao.create(harvest)
            second <- climbDao.update(Revisioned[Climb](first.revision, newHarvest))
          } yield (first, second)
        } getOrElse fail("Failed to create fixtures")

        val rev = run {
          climbDao.update(Revisioned[Climb](first.revision, harvest))
        }.swap getOrElse fail("Managed to update climb despite old revision")

        rev should equal (EditConflict())
      }

      it("should inform if the crag is being updated concurrently in an other transaction") (pending)

      it("should bump the Crag's revision number when updating the Climb") {
        val (first, second) = run {
          for {
            first  <- CragDao.create(burbage)
            _      <- climbDao.create(harvest)
            second <- CragDao.get("burbage")
          } yield (first, second)
        } getOrElse fail("Failed to create crag fixture")

        first.revision should be < (second.revision)
      }
    }

    describe("The delete action") {
      it("should delete an existing climb successfully") (pending)
      it("should be possible to create a new climb with the same name of previously deleted climb") (pending)
      it("should inform if the climb has been updated concurrently") (pending)
      it("should inform if the climb has been deleted concurrently") (pending)
      it("should bump the revision number of the Crag that the climb belonged to") (pending)
    }

    describe("The history action") {
      it("should return an existing climb's history of edits") (pending)
      it("should return the empty list for a climb that doesn't exist") (pending)
      it("should return the empty list for a climb that has been deleted") (pending)
      it("should not return the old history for a new climb that overwrites an old climb") (pending)
    }

    describe("The deleted list action") {
      it("should be empty if no climbs have been deleted") (pending)
      it("should contain only the latest revision of a deleted climb") (pending)
    }

    describe("The purge action") {
      it("should remove the climb and its history") (pending)
      it("should be possible to create a new climb with the same name as a previsously purged climb") (pending)
      it("should inform if the climb has been updated concurrently") (pending)
      it("should inform if the climb has been purged concurrently") (pending)
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

