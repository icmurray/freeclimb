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
      val climb = blockFor {
        module.climbs.create("Right Unconquerable")
      }
      climb.isSuccess should equal (true)

      val reRead = blockFor {
        module.climbs.withId(climb.toOption.get.id)
      }

      reRead should not equal (None)
      reRead.get.name should equal ("Right Unconquerable")
    }
  }

  "A ClimbService" should "merge 2 climbs" in {
    withClimbsModule { module =>
      implicit val ec = module.ec
      val (climb1, climb2) = blockFor {
        for {
          c1 <- module.climbs.create("Climb 1")
          c2 <- module.climbs.create("Climb 2")
        } yield (c1, c2)
      }

      blockFor {
        module.climbs.deDuplicate(Keep(climb1.toOption.get.id), Remove(climb2.toOption.get.id))
      }

      val climb2Again = blockFor {
        module.climbs.withId(climb2.toOption.get.id)
      }
      climb2Again should equal (None)

      val resolved = blockFor {
        module.climbs.resolvesTo(climb2.toOption.get.id)
      }
      resolved should equal (climb1.toOption)
    }
  }

  private def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  private def withClimbsModule(f: ClimbsModule[Future] with ActorSystemModule => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new ActorClimbsModule with ActorSystemModule {
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
