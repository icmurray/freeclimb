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

/**
 * Specification for ClimbDao
 */
trait ClimbDaoSpec extends FunSpec
                   with BeforeAndAfter
                   with ShouldMatchers {

  val climbDao: ClimbDao = ClimbDao

  def runner: ActionRunner

  def withDaos(testCode: (CragDao, ClimbDao) => Any): Unit

  private def run[M[+_], A, I <: IsolationLevel](action: ApiAction[A, I])(implicit m: Manifest[I]) = {
    runner.run(action)
  }

  describe("Climb DAO") {

    describe("The get action") {
      it("should return the latest version of a climb that exists") {
        withDaos { (cragDao, climbDao) =>
          // Create the crags
          run {
            cragDao.create(burbage)
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

      it("should return NotFound if the climb does not exist") {
        withDaos { (cragDao, climbDao) =>
          val someClimb = run {
            climbDao.get("burbage", "harvest")
          }.swap getOrElse fail("Managed to find non-existeant Climb!")

          someClimb should equal (NotFound())
        }
      }

      it("should return NotFound if the climb's crag does not exist") {

        withDaos { (cragDao, climbDao) =>
          // create harvest at burbage
          run {
            for {
              _ <- cragDao.create(burbage)
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
    }

    describe("The getOption action") {
      it("should return the latest version of a climb that exists") {
        withDaos { (cragDao, climbDao) =>
          // Create the crags
          run {
            cragDao.create(burbage)
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
      }

      it("should return None if the climb does not exist") {
        withDaos { (cragDao, climbDao) =>
          val someClimb = run {
            climbDao.getOption("burbage", "harvest")
          } getOrElse fail("Failed to run getOption")

          someClimb should equal (None)
        }
      }

      it("should return None if the climb's crag does not exist") {

        withDaos { (cragDao, climbDao) =>
          // create harvest at burbage
          run {
            for {
              _ <- cragDao.create(burbage)
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
    }

    describe("The create action") {
      
      it("should successfuly create a new climb") {
        withDaos { (cragDao, climbDao) =>
          run {
            cragDao.create(burbage)
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
      
      it("should not create a climb if it already exists") {

        withDaos { (cragDao, climbDao) =>
          run {
            cragDao.create(burbage)
          } getOrElse fail("Failed to create fixture crag")

          // Create the Crag initially.
          run { climbDao.create(harvest) } getOrElse fail ("Failed to create fixture")

          // Now try to re-create it
          run {
            climbDao.create(harvest)
          }.swap getOrElse fail("Climb was re-created")
        }
      }

      it("should not create a climb if the crag does not exist") {
        withDaos { (cragDao, climbDao) =>
          val error = run {
            climbDao.create(harvest)
          }.swap getOrElse fail("Managed to create a climb without a crag!")

          error should equal (ValidationError())
        }
      }

      it("should be possible to have two climbs with the same name on different crags") {
        withDaos { (cragDao, climbDao) =>
          run {
            for {
              b <- cragDao.create(burbage)
              s <- cragDao.create(stanage)
            } yield ()
          } getOrElse fail("Unable to create fixture crags")

          run { climbDao.create(harvest) } getOrElse fail("Unable to create fixture climb")

          run {
            climbDao.create(newerHarvest)
          } getOrElse fail("Couldn't create climb with same name on different crag")
        }
      }

      it("should bump the revision number of the crag that the climb is created against") {
        withDaos { (cragDao, climbDao) =>
          val originalCragRev = run {
            cragDao.create(burbage)
          } getOrElse fail("Failed to create crag fixture")

          run {
            climbDao.create(harvest)
          } getOrElse fail("Couldn't create climb fixture")

          val newCragRev = run { cragDao.get("burbage") } getOrElse fail("Failed to retrieve crag")

          newCragRev.revision should be > (originalCragRev.revision)
        }
      }
    }

    describe("The update action") {
      it("should update an existing climb successfully") {
        withDaos { (cragDao, climbDao) =>
          val (first, second) = run {
            for {
              _ <- cragDao.create(burbage)
              first <- climbDao.create(harvest)
              second <- climbDao.update(Revisioned[Climb](first.revision, newHarvest))
            } yield (first, second)
          } getOrElse fail("Failed to create fixtures")

          first.revision should be < (second.revision)
          second.model should equal (newHarvest)
        }
      }

      it("should inform if the climb has been updated concurrently") {
        withDaos { (cragDao, climbDao) =>
          val (first, second) = run {
            for {
              _ <- cragDao.create(burbage)
              first <- climbDao.create(harvest)
              second <- climbDao.update(Revisioned[Climb](first.revision, newHarvest))
            } yield (first, second)
          } getOrElse fail("Failed to create fixtures")

          val rev = run {
            climbDao.update(Revisioned[Climb](first.revision, harvest))
          }.swap getOrElse fail("Managed to update climb despite old revision")

          rev should equal (EditConflict())
        }
      }

      it("should bump the Crag's revision number when updating the Climb") {
        withDaos { (cragDao, climbDao) =>
          val (first, second) = run {
            for {
              first  <- cragDao.create(burbage)
              _      <- climbDao.create(harvest)
              second <- cragDao.get("burbage")
            } yield (first, second)
          } getOrElse fail("Failed to create crag fixture")

          first.revision should be < (second.revision)
        }
      }
    }

    describe("The delete action") {
      it("should delete an existing climb successfully") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _      <- cragDao.create(burbage)
              rev    <- climbDao.create(harvest)
              _      <- climbDao.delete(rev)
              result <- climbDao.getOption("burbage", "harvest")
            } yield result
          } getOrElse fail("Failed to delete climb")

          result should equal (None)
        }
      }

      it("should be possible to create a new climb with the same name of previously deleted climb") {
        withDaos { (cragDao, climbDao) =>
          val (first, second) = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.delete(rev)
              result <- climbDao.create(newHarvest)
            } yield (rev, result)
          } getOrElse fail("Failed to re-create climb")

          first.revision should be < second.revision
          second.model should equal (newHarvest)
        }
      }

      it("should inform if the climb has been updated concurrently") {
        withDaos { (cragDao, climbDao) =>
          val climbRev = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
            } yield rev
          } getOrElse fail("Failed to create Climb")

          val result = run {
            climbDao.delete(Revisioned[Climb](climbRev.revision-1, climbRev.model))
          }.swap getOrElse fail("Purge should have failed")
        }
      }

      it("should inform if the climb has been deleted concurrently") {
        withDaos { (cragDao, climbDao) =>
          val climbRev = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.delete(rev)
            } yield rev
          } getOrElse fail("Failed to create Climb")

          val result = run {
            climbDao.delete(climbRev)
          }.swap getOrElse fail("Purge should have failed")
        }
      }

      it("should bump the revision number of the Crag that the climb belonged to") {
        withDaos { (cragDao, climbDao) =>
          val (first, second) = run {
            for {
              _      <- cragDao.create(burbage)
              climb  <- climbDao.create(harvest)
              first  <- cragDao.get("burbage")
              _      <- climbDao.delete(climb)
              second <- cragDao.get("burbage")
            } yield (first, second)
          } getOrElse fail("Failed to setup fixtures")

          first.revision should be < (second.revision)
        }
      }
    }

    describe("The history action") {
      it("should return an existing climb's history of edits") {
        withDaos { (cragDao, climbDao) =>
          val (rev1, rev2) = run {
            for {
              _    <- cragDao.create(burbage)
              rev1 <- climbDao.create(harvest)
              rev2 <- climbDao.update(Revisioned[Climb](rev1.revision, newHarvest))
            } yield (rev1, rev2)
          } getOrElse fail ("Couldn't create climb fixture")

          val history = run {
            climbDao.history(harvest)
          } getOrElse fail("Couldn't retrieve history")

          history.toList should equal (List(rev2, rev1))
        }
      }

      it("should return the empty list for a climb that doesn't exist") {
        withDaos { (cragDao, climbDao) =>
          val history = run {
            climbDao.history(harvest)
          } getOrElse fail("Couldn't retrieve history")

          history.toList should equal (Nil)
        }
      }

      it("should return the empty list for a climb that has been deleted") {

        withDaos { (cragDao, climbDao) =>
          run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.delete(rev)
            } yield ()
          } getOrElse fail("Could not create fixtures")

          val history = run {
            climbDao.history(harvest)
          } getOrElse fail("Couldn't retrieve history")

          history.toList should equal (Nil)
        }
      }

      it("should not return the old history for a new climb that overwrites an old climb") {
        withDaos { (cragDao, climbDao) =>
          run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.delete(rev)
            } yield ()
          } getOrElse fail("Could not create fixtures")

          val newRev = run {
            climbDao.create(newHarvest)
          } getOrElse fail("Couldn't re-create crag")

          val history = run {
            climbDao.history(harvest)
          } getOrElse fail("Couldn't retrieve history")

          history.toList should equal (List(newRev))
        }
      }
    }

    describe("The deleted list action") {
      it("should be empty if no climbs have been deleted") {
        withDaos { (cragDao, climbDao) =>
          val deletedList = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.update(Revisioned[Climb](rev.revision, newHarvest))
              del <- climbDao.deletedList()
            } yield del
          } getOrElse fail("Could not create fixtures")

          deletedList.toList should equal (Nil)
        }
      }

      it("should contain only the latest revision of a deleted climb") {
        withDaos { (cragDao, climbDao) =>
          // Create a climb, update it once, then delete it
          val (rev1, rev2) = run {
            for {
              _    <- cragDao.create(burbage)
              rev1 <- climbDao.create(harvest)
              rev2 <- climbDao.update(Revisioned[Climb](rev1.revision, newHarvest))
              _    <- climbDao.delete(rev2)
            } yield (rev1, rev2)
          } getOrElse fail("Error setting up fixture")

          // Check the deleted list
          val deletedList = run {
            climbDao.deletedList()
          } getOrElse fail("Error retrieving deleted list")

          deletedList.toList.map { _.model } should contain (newHarvest)
          deletedList.toList.map { _.model } should not contain (harvest)
        }
      }

      it("should return a deleted climb even if the crag has been deleted") {
        withDaos { (cragDao, climbDao) =>
          val (cragRev, rev1, rev2) = run {
            for {
              _       <- cragDao.create(burbage)
              rev1    <- climbDao.create(harvest)
              rev2    <- climbDao.update(Revisioned[Climb](rev1.revision, newHarvest))
              _       <- climbDao.delete(rev2)
              cragRev <- cragDao.get("burbage")
              _       <- cragDao.delete(cragRev)
            } yield (cragRev, rev1, rev2)
          } getOrElse fail("Error creating fixtures")

          // Check the deleted list
          val deletedList = run {
            climbDao.deletedList()
          } getOrElse fail("Error retrieving deleted list")

          deletedList.toList.map { _.model } should equal (List(newHarvest))
        }
      }

      it("should return the *latest* crag along with the climb") {
        withDaos { (cragDao, climbDao) =>
          // Create a new climb, update it, and update the crag it belongs to.
          val (cragRev, rev1, rev2) = run {
            for {
              _        <- cragDao.create(burbage)
              rev1     <- climbDao.create(harvest)
              rev2     <- climbDao.update(Revisioned[Climb](rev1.revision, newHarvest))
              _        <- climbDao.delete(rev2)
              origCrag <- cragDao.get("burbage")
              cragRev  <- cragDao.update(Revisioned[Crag](origCrag.revision, newBurbage))
            } yield (cragRev, rev1, rev2)
          } getOrElse fail("Error creating fixtures")

          // Check the deleted list contains the deleted crag, but that it references
          // the *latest* crag, not the crag it originally referenced.
          val deletedList = run {
            climbDao.deletedList()
          } getOrElse fail("Error retrieving deleted list")

          deletedList.length should equal (1)
          deletedList.toList.head.model.crag should equal (cragRev.model)
        }
      }
    }

    describe("The purge action") {
      it("should remove the climb and its history") {
        withDaos { (cragDao, climbDao) =>
          val someClimb = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.purge(rev)
              del <- climbDao.getOption("burbage", "harvest")
            } yield (del)
          } getOrElse fail("Failed to create fixture")

          // Check it was deleted
          someClimb should equal (None)

          // Check it's not in the deletedClimbs list
          val deletedList = run {
            climbDao.deletedList()
          } getOrElse fail("Error retrieving deletedList")

          deletedList should equal (Nil)
        }
      }

      it("should be possible to create a new climb with the same name as a previsously purged climb") {
        withDaos { (cragDao, climbDao) =>
          val newRev = run {
            for {
              _    <- cragDao.create(burbage)
              rev1 <- climbDao.create(harvest)
              _    <- climbDao.purge(rev1)
              rev2 <- climbDao.create(newHarvest)
            } yield rev2
          } getOrElse fail("Failed to create fixtures")

          newRev.model should equal (newHarvest)
        }
      }

      it("should inform if the climb has been updated concurrently") {
        withDaos { (cragDao, climbDao) =>
          val climbRev = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
            } yield rev
          } getOrElse fail("Failed to create Climb")

          val result = run {
            climbDao.purge(Revisioned[Climb](climbRev.revision-1, climbRev.model))
          }.swap getOrElse fail("Purge should have failed")

          result should equal (EditConflict())
        }
      }

      it("should inform if the climb has been purged concurrently") {
        withDaos { (cragDao, climbDao) =>
          val climbRev = run {
            for {
              _   <- cragDao.create(burbage)
              rev <- climbDao.create(harvest)
              _   <- climbDao.purge(rev)
            } yield rev
          } getOrElse fail("Failed to create and purge Climb")

          val result = run {
            climbDao.purge(climbRev)
          }.swap getOrElse fail("Delete should have failed")

          result should equal (NotFound())
        }
      }
    }

    describe("the climbsCreatedOrUpdatedSince action") {
      it("should return empty for the latest revision of a crag") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _ <- cragDao.create(burbage)
              r <- climbDao.create(harvest)
              _ <- climbDao.update(Revisioned[Climb](r.revision, newHarvest))
              _ <- climbDao.create(sorb)
              c <- cragDao.get("burbage")
              result <- climbDao.climbsCreatedOrUpdatedSince(c)
            } yield result
          } getOrElse fail("Failed to setup fixtures")

          result should equal (Nil)
        }
      }

      it("should return empty if the crag doesn't exist") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            climbDao.climbsCreatedOrUpdatedSince(Revisioned[Crag](1, burbage))
          } getOrElse fail("Failed to retrieve list of updates with non-existant crag")

          result should equal (Nil)
        }
      }

      it("should return a climb created since last revision") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _       <- cragDao.create(burbage)
              _       <- climbDao.create(harvest)
              cragRev <- cragDao.get("burbage")
              _       <- climbDao.create(sorb)
              result  <- climbDao.climbsCreatedOrUpdatedSince(cragRev)
            } yield result
          } getOrElse fail("Failed to setup fixtures")

          result.map { _.model } should not contain (harvest)
          result.map { _.model } should contain (sorb)
          result.length should equal (1)
        }
      }

      it("should return the latest revision of a climb updated since crag was created") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _       <- cragDao.create(burbage)
              _       <- climbDao.create(sorb)
              rev     <- climbDao.create(harvest)
              cragRev <- cragDao.get("burbage")
              _       <- climbDao.update(Revisioned[Climb](rev.revision, newHarvest))
              result  <- climbDao.climbsCreatedOrUpdatedSince(cragRev)
            } yield result
          } getOrElse fail("Failed to setup fixtures")

          result.map { _.model } should contain (newHarvest)
          result.map { _.model } should not contain (sorb)
          result.length should equal (1)
        }
      }
    }

    describe("the climbsDeletedSince action") {
      it("should return empty for the latest revision of a crag") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _ <- cragDao.create(burbage)
              r <- climbDao.create(harvest)
              _ <- climbDao.update(Revisioned[Climb](r.revision, newHarvest))
              _ <- climbDao.create(sorb)
              c <- cragDao.get("burbage")
              result <- climbDao.climbsDeletedSince(c)
            } yield result
          } getOrElse fail("Failed to setup fixtures")

          result should equal (Nil)
        }
      }

      it("should return empty if the crag doesn't exist") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            climbDao.climbsDeletedSince(Revisioned[Crag](1, burbage))
          } getOrElse fail("Failed to retrieve list of updates with non-existant crag")

          result should equal (Nil)
        }
      }

      it("should return the last version of a climb deleted since the given crag revision") {
        withDaos { (cragDao, climbDao) =>
          val result = run {
            for {
              _       <- cragDao.create(burbage)
              _       <- climbDao.create(sorb)
              rev     <- climbDao.create(harvest)
              cragRev <- cragDao.get("burbage")
              _       <- climbDao.delete(rev)
              result  <- climbDao.climbsDeletedSince(cragRev)
            } yield result
          } getOrElse fail("Failed to setup fixtures")

          result.map { _.model } should contain (harvest)
          result.length should equal (1)
        }
      }
    }

  }
  
  private val burbage = Crag.makeUnsafe("burbage", "Burbage")
  private val newBurbage = Crag.makeUnsafe("burbage", "BURBAGE")
  private val stanage = Crag.makeUnsafe("stanage", "Stanage")

  private val harvest = Climb.makeUnsafe(
    "harvest",
    "Harvest",
    "It is brutal, and on the wrong crag.",
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

  private val sorb = Climb.makeUnsafe(
    "sorb",
    "Sorb",
    "It's Sorb",
    burbage,
    UkTrad(Grade.UkAdjective.E2, Grade.UkTechnical.T5c))

}

class ClimbDaoTest extends ClimbDaoSpec {
  override val runner = new DefaultActionRunner(TestDatabaseSessions.source)
  
  override def withDaos(testCode: (CragDao, ClimbDao) => Any) {
    try {
      cleanTables()
      testCode(CragDao, ClimbDao)
    } finally {
      cleanTables()
    }
  }

  private def cleanTables() {
    implicit val connection = TestDatabaseSessions.newConnection(new TransactionRepeatableRead())
    SQL("DELETE FROM climbs;").execute()
    SQL("DELETE FROM climb_history;").execute()
    SQL("DELETE FROM crags;").execute()
    SQL("DELETE FROM crag_history;").execute()
    connection.commit()
    connection.close()
  }

  describe("The SQL-backed ClimbDao") {
      it("should inform if the crag is being updated concurrently in an other transaction") (pending)

      it("should return a concurrent update if climbs with the same name are created at the same time") (pending)
  }
}
