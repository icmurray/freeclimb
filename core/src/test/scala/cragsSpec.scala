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

class CragServiceSpec extends FlatSpec with ShouldMatchers with MockFactory {

  "A CragService" should "create new crags" in {
    withCragsModule { implicit module =>
      val cragId = blockFor {
        module.crags.create("Stanage", "A pretty nice crag")
      }
      cragId.isSuccess should equal (true)

      val crag = blockFor {
        module.crags.withId(cragId.toOption.get)
      }

      crag should not equal (None)
      crag.get.name should equal ("Stanage")

      val allCrags = blockFor {
        module.crags.list()
      }

      allCrags.length should equal (1)
      allCrags(0).name should equal ("Stanage")
    }
  }

  // For brevity...
  type ModuleUnderTest = CragsModule[Future] with ActorSystemModule

  private def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  private def withCragsModule(f: ModuleUnderTest => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new EventsourcedCragsModule with ActorSystemModule {
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
