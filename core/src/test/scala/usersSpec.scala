package org.freeclimbers.core

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

import com.typesafe.config.ConfigFactory

class UserServiceSpec extends FlatSpec with ShouldMatchers {

  "A UserService" should "register new users" in {
    withUsersModule { module =>
      val userF = module.users.register(
        Email("test@example.com"), "Test", "User", PlainText("pass"))
      val user = Await.result(userF, 2.seconds)
      user.isSuccess should equal (true)
    }
  }

  "A UserService" should "not re-register an existing user" in {
    withUsersModule { module =>
      val userF = module.users.register(
        Email("test@example.com"), "Test", "User", PlainText("pass"))
      val user = Await.result(userF, 1.seconds)
      user.isSuccess should equal (true)

      val sameUserF = module.users.register(
        Email(" test@example.com "), "Test", "User", PlainText("pass"))
      val sameUser = Await.result(sameUserF, 1.seconds)
      sameUser.isSuccess should equal(false)
    }
  }

  "A UserService" should "not allow blank emails" in {
    withUsersModule { module =>
      val userF = module.users.register(
        Email("  "), "Test", "User", PlainText("pass"))
      val user = Await.result(userF, 1.seconds)
      user.isSuccess should equal (false)
    }
  }

  //trait TestUsersModule extends InMemoryUsersModule[Id] {
  //  val M = Monad[Id]
  //}

  private def withUsersModule(f: UsersModule[Future] => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new ActorUsersModule {
        lazy val actorSystem = system
        lazy val M = Monad[Future]
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

class NullJournal extends Actor with SyncWriteJournal {
  import scala.concurrent.ExecutionContext.Implicits.global
  def write(persistent: PersistentImpl): Unit = {}
  def writeBatch(persistentBatch: Seq[PersistentImpl]): Unit = {}
  def delete(persistent: PersistentImpl): Unit = {}
  def confirm(processorId: String, sequenceNr: Long, channelId: String): Unit = {}
  def replayAsync(processorId: String, fromSequenceNr: Long, toSequenceNr: Long)
                 (replayCallback: PersistentImpl => Unit)
                 : Future[Long] = future { 0L }
}

