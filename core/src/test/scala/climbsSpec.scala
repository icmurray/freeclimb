package org.freeclimbers
package core

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.{ActorSystem, Actor}
import akka.testkit.TestKit
import akka.persistence._
import akka.persistence.journal._

import org.scalamock.scalatest.MockFactory

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import scalaz._
import scalaz.Id._
import scalaz.contrib.std.scalaFuture._
import scalaz.contrib.std.scalaFuture

import com.typesafe.config.ConfigFactory

class ClimbServiceSpec extends FlatSpec with ShouldMatchers with MockFactory {

  "A ClimbService" should "create new climbs" in {
    withClimbsModule { implicit module =>
      withMockCrag { cragId =>

        val climbId = blockFor {
          module.routesDB.createClimb("Right Unconquerable", "A pretty nice climb", cragId)
        }
        climbId.isSuccess should equal (true)

        val climb = blockFor {
          module.routesDB.climbById(climbId.toOption.get)
        }

        climb should not equal (None)
        climb.get.name should equal ("Right Unconquerable")
      }
    }
  }

  "A ClimbService" should "merge 2 climbs" in {
    withClimbsModule { implicit module =>
      withMockCrag { cragId =>
        implicit val ec = module.ec
        val (climbId1, climbId2) = blockFor {
          for {
            c1 <- module.routesDB.createClimb("Climb 1", "", cragId)
            c2 <- module.routesDB.createClimb("Climb 2", "", cragId)
          } yield (c1.toOption.get, c2.toOption.get)
        }

        blockFor {
          module.routesDB.mergeClimbs(Keep(climbId1), Remove(climbId2))
        }

        val (climb1, climb2) = blockFor {
          for {
            c1 <- module.routesDB.climbById(climbId1)
            c2 <- module.routesDB.climbById(climbId2)
          } yield (c1, c2)
        }
        climb2 should equal (None)

        val resolved = blockFor {
          module.routesDB.resolveClimb(climbId2)
        }
        resolved should equal (climb1)
      }
    }
  }
  
  "A ClimbService" should "not follow redirect chains" in {
    withClimbsModule { implicit module =>
      withMockCrag { cragId =>
        implicit val ec = module.ec
        val (climbId1, climbId2, climbId3) = blockFor {
          for {
            c1 <- module.routesDB.createClimb("Climb 1", "", cragId)
            c2 <- module.routesDB.createClimb("Climb 2", "", cragId)
            c3 <- module.routesDB.createClimb("Climb 3", "", cragId)
          } yield (c1.toOption.get, c2.toOption.get, c3.toOption.get)
        }

        val (r1,r2) = blockFor {
          for {
            r1 <- module.routesDB.mergeClimbs(Keep(climbId1), Remove(climbId2))
            r2 <- module.routesDB.mergeClimbs(Keep(climbId2), Remove(climbId3))
          } yield (r1,r2)
        }
        r1.isSuccess should equal (true)
        r2.isSuccess should equal (false)
      }
    }
  }
  
  "A ClimbService" should "not allow redirecting to oneself" in {
    withClimbsModule { implicit module =>
      withMockCrag { cragId =>
        implicit val ec = module.ec
        val climbId = blockFor {
          for {
            c1 <- module.routesDB.createClimb("Climb 1", "", cragId)
          } yield c1.toOption.get
        }

        val result = blockFor {
          module.routesDB.mergeClimbs(Keep(climbId), Remove(climbId))
        }
        result.isSuccess should equal (false)
      }
    }
  }

  "A ClimbService" should "re-establish existing redirects" in {
    withClimbsModule { implicit module =>
      withMockCrag { cragId =>
        implicit val ec = module.ec
        val (climbId1, climbId2, climbId3, climbId4) = blockFor {
          for {
            c1 <- module.routesDB.createClimb("Climb 1", "", cragId)
            c2 <- module.routesDB.createClimb("Climb 2", "", cragId)
            c3 <- module.routesDB.createClimb("Climb 3", "", cragId)
            c4 <- module.routesDB.createClimb("Climb 4", "", cragId)
          } yield (c1.toOption.get, c2.toOption.get, c3.toOption.get, c4.toOption.get)
        }

        val (r1,r2) = blockFor {
          for {
            r1 <- module.routesDB.mergeClimbs(Keep(climbId1), Remove(climbId3))
            r2 <- module.routesDB.mergeClimbs(Keep(climbId2), Remove(climbId4))
          } yield (r1,r2)
        }
        r1.isSuccess should equal (true)
        r2.isSuccess should equal (true)

        val r = blockFor {
          module.routesDB.mergeClimbs(Keep(climbId1), Remove(climbId2))
        }
        r.isSuccess should equal (true)

        val (id1, id2, id3, id4) = blockFor {
          for {
            c1 <- module.routesDB.climbById(climbId1)
            c2 <- module.routesDB.climbById(climbId2)
            c3 <- module.routesDB.climbById(climbId3)
            c4 <- module.routesDB.climbById(climbId4)
          } yield (c1.map(_.id), c2.map(_.id), c3.map(_.id), c4.map(_.id))
        }
        id1 should equal (Some(climbId1))
        id2 should equal (None)
        id3 should equal (None)
        id4 should equal (None)

        val (ln2, ln3, ln4) = blockFor {
          for {
            ln2 <- module.routesDB.resolveClimb(climbId2)
            ln3 <- module.routesDB.resolveClimb(climbId3)
            ln4 <- module.routesDB.resolveClimb(climbId4)
          } yield (ln2, ln3, ln4)
        }

        ln2.map(_.id) should equal (Some(climbId1))
        ln3.map(_.id) should equal (Some(climbId1))
        ln4.map(_.id) should equal (Some(climbId1))
      }
    }
  }

  "A ClimbService" should "not be able to create circular redirect links" in {
    (pending) // scalacheck?
  }

  "A ClimbService" should "not create climbs in crags that don't exist" in {
    withClimbsModule { implicit module =>
      implicit val ec = module.ec
      val cragId = CragId.createRandom()

      val climbId = blockFor {
        module.routesDB.createClimb("Right Unconquerable", "A pretty nice climb", cragId)
      }
      climbId.isFailure should equal (true)

    }
  }

  // For brevity...
  type ModuleUnderTest = RoutesDatabaseModule[Future] with ActorSystemModule

  private def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  private def withClimbsModule(f: ModuleUnderTest => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new EventsourcedRoutesDatabaseModule with ActorSystemModule {
        implicit def M = scalaFuture.futureInstance
        override lazy val actorSystem = system
      }
      f(module)
    } finally {
      system.shutdown()
    }
  }

  private def withMockCrag(f: CragId => Unit)(implicit module: ModuleUnderTest) = {
    implicit val ec = module.ec
    val cragF = module.routesDB.createCrag("A Crag", "A Crag Description")
    cragF.map(_.map(f))
  }

  private lazy val unitTestConfig = {
    val journalConfig = ConfigFactory.parseString(
    """
    {
      akka.persistence.journal.plugin = "null_journal"

      null_journal {
        class = "org.freeclimbers.core.NullJournal"
        plugin-dispatcher = "akka.actor.default-dispatcher"
      }

      akka.log-dead-letters-during-shutdown = off
    }
    """)

    journalConfig withFallback ConfigFactory.load()
  }

}
