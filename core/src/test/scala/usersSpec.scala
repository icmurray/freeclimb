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
      val user = blockFor {
        module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass"))
      }
      user.isSuccess should equal (true)
    }
  }

  "A UserService" should "not re-register an existing user" in {
    withUsersModule { module =>
      val user = blockFor {
        module.users.register( Email("test@example.com"), "Test", "User", PlainText("pass"))
      }
      user.isSuccess should equal (true)

      val sameUser = blockFor {
        module.users.register(Email(" test@example.com "), "Different", "Name", PlainText("pass"))
      }
      sameUser.isSuccess should equal(false)
    }
  }

  "A UserService" should "not allow blank emails" in {
    withUsersModule { module =>
      val user = blockFor {
        module.users.register(Email("  "), "Test", "User", PlainText("pass"))
      }
      user.isSuccess should equal (false)
    }
  }

  "A UserService" should "allow a user to authenticate with password" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val auth = blockFor {
        for {
          userV <- module.users.register(
            Email("test@example.com"), "Test", "User", PlainText("pass")
          )
          user = userV.getOrElse(throw new RuntimeException())

          auth <- module.users.authenticate(user.email, PlainText("pass"))
        } yield auth
      }

      auth should not equal (None)
      auth.get.email should equal (Email("test@example.com"))
    }
  }

  "A UserService" should "not allow a user to authenticate with the wrong password" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val auth = blockFor {
        for {
          userV <- module.users.register(
            Email("test@example.com"), "Test", "User", PlainText("pass")
          )
          user = userV.getOrElse(throw new RuntimeException())

          auth <- module.users.authenticate(user.email, PlainText("WRONG"))
        } yield auth
      }

      auth should equal (None)
    }
  }

  "A UserService" should "allow users to log in and out" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val token = blockFor {
        for {
          userV <- module.users.register(
            Email("test@example.com"), "Test", "User", PlainText("pass")
          )
          user = userV.getOrElse(throw new RuntimeException())

          tokenO <- module.users.login(user.email, PlainText("pass"))
        } yield tokenO.get
      }

      val auth = blockFor {
        module.users.authenticate(token)
      }

      auth should not equal (None)
      auth.get.email should equal (Email("test@example.com"))

      blockFor {
        module.users.logout(token)
      }

      val authAgain = blockFor {
        module.users.authenticate(token)
      }
      authAgain should equal (None)
    }
  }

  private def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  private def withUsersModule(f: UsersModule[Future] with ActorSystemModule => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new ActorUsersModule with ActorSystemModule {
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

