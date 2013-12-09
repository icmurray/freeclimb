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
