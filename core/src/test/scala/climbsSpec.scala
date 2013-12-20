package org.freeclimbers
package core

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.{ActorSystem, Actor}
import akka.testkit.TestKit
import akka.persistence._
import akka.persistence.journal._

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import scalaz._
import scalaz.Id._
import scalaz.contrib.std.scalaFuture._
import scalaz.contrib.std.scalaFuture

import com.typesafe.config.ConfigFactory

class ClimbServiceSpec extends FlatSpec with ShouldMatchers {

  "A ClimbService" should "create new climbs" in {
    withClimbsModule { module =>
      val climbId = blockFor {
        module.climbs.create("Right Unconquerable", "A pretty nice climb", CragId.createRandom())
      }
      climbId.isSuccess should equal (true)

      val climb = blockFor {
        module.climbs.withId(climbId.toOption.get)
      }

      climb should not equal (None)
      climb.get.name should equal ("Right Unconquerable")
    }
  }

  "A ClimbService" should "merge 2 climbs" in {
    withClimbsModule { module =>
      implicit val ec = module.ec
      val (climbId1, climbId2) = blockFor {
        for {
          c1 <- module.climbs.create("Climb 1", "", CragId.createRandom())
          c2 <- module.climbs.create("Climb 2", "", CragId.createRandom())
        } yield (c1.toOption.get, c2.toOption.get)
      }

      blockFor {
        module.climbs.deDuplicate(Keep(climbId1), Remove(climbId2))
      }

      val (climb1, climb2) = blockFor {
        for {
          c1 <- module.climbs.withId(climbId1)
          c2 <- module.climbs.withId(climbId2)
        } yield (c1, c2)
      }
      climb2 should equal (None)

      val resolved = blockFor {
        module.climbs.resolvesTo(climbId2)
      }
      resolved should equal (climb1)
    }
  }
  
  "A ClimbService" should "not follow redirect chains" in {
    withClimbsModule { module =>
      implicit val ec = module.ec
      val (climbId1, climbId2, climbId3) = blockFor {
        for {
          c1 <- module.climbs.create("Climb 1", "", CragId.createRandom())
          c2 <- module.climbs.create("Climb 2", "", CragId.createRandom())
          c3 <- module.climbs.create("Climb 3", "", CragId.createRandom())
        } yield (c1.toOption.get, c2.toOption.get, c3.toOption.get)
      }

      val (r1,r2) = blockFor {
        for {
          r1 <- module.climbs.deDuplicate(Keep(climbId1), Remove(climbId2))
          r2 <- module.climbs.deDuplicate(Keep(climbId2), Remove(climbId3))
        } yield (r1,r2)
      }
      r1.isSuccess should equal (true)
      r2.isSuccess should equal (false)
    }
  }
  
  "A ClimbService" should "not allow redirecting to oneself" in {
    withClimbsModule { module =>
      implicit val ec = module.ec
      val climbId = blockFor {
        for {
          c1 <- module.climbs.create("Climb 1", "", CragId.createRandom())
        } yield c1.toOption.get
      }

      val result = blockFor {
        module.climbs.deDuplicate(Keep(climbId), Remove(climbId))
      }
      result.isSuccess should equal (false)
    }
  }

  "A ClimbService" should "re-establish existing redirects" in {
    withClimbsModule { module =>
      implicit val ec = module.ec
      val (climbId1, climbId2, climbId3, climbId4) = blockFor {
        for {
          c1 <- module.climbs.create("Climb 1", "", CragId.createRandom())
          c2 <- module.climbs.create("Climb 2", "", CragId.createRandom())
          c3 <- module.climbs.create("Climb 3", "", CragId.createRandom())
          c4 <- module.climbs.create("Climb 4", "", CragId.createRandom())
        } yield (c1.toOption.get, c2.toOption.get, c3.toOption.get, c4.toOption.get)
      }

      val (r1,r2) = blockFor {
        for {
          r1 <- module.climbs.deDuplicate(Keep(climbId1), Remove(climbId3))
          r2 <- module.climbs.deDuplicate(Keep(climbId2), Remove(climbId4))
        } yield (r1,r2)
      }
      r1.isSuccess should equal (true)
      r2.isSuccess should equal (true)

      val r = blockFor {
        module.climbs.deDuplicate(Keep(climbId1), Remove(climbId2))
      }
      r.isSuccess should equal (true)

      val (id1, id2, id3, id4) = blockFor {
        for {
          c1 <- module.climbs.withId(climbId1)
          c2 <- module.climbs.withId(climbId2)
          c3 <- module.climbs.withId(climbId3)
          c4 <- module.climbs.withId(climbId4)
        } yield (c1.map(_.id), c2.map(_.id), c3.map(_.id), c4.map(_.id))
      }
      id1 should equal (Some(climbId1))
      id2 should equal (None)
      id3 should equal (None)
      id4 should equal (None)

      val (ln2, ln3, ln4) = blockFor {
        for {
          ln2 <- module.climbs.resolvesTo(climbId2)
          ln3 <- module.climbs.resolvesTo(climbId3)
          ln4 <- module.climbs.resolvesTo(climbId4)
        } yield (ln2, ln3, ln4)
      }

      ln2.map(_.id) should equal (Some(climbId1))
      ln3.map(_.id) should equal (Some(climbId1))
      ln4.map(_.id) should equal (Some(climbId1))

    }
  }

  "A ClimbService" should "not be able to create circular redirect links" in {
    (pending) // scalacheck?
  }

  private def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  private def withClimbsModule(f: ClimbsModule[Future] with ActorSystemModule => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new EventsourcedClimbsModule with ActorSystemModule {
        implicit def M = scalaFuture.futureInstance
        override lazy val actorSystem = system
      }
      f(module)
    } finally {
      system.shutdown()
    }
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
    }
    """)

    journalConfig withFallback ConfigFactory.load()
  }

}
