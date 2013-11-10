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

  "A UserService" should "allow a user to authenticate with password" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val authF = for {
        userV <- module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass")
        )
        user = userV.getOrElse(throw new RuntimeException())

        auth <- module.users.authenticate(user.email, PlainText("pass"))
      } yield auth

      val auth = Await.result(authF, 2.seconds)
      auth should not equal (None)
      auth.get.email should equal (Email("test@example.com"))
    }
  }

  "A UserService" should "not allow a user to authenticate with the wrong password" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val authF = for {
        userV <- module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass")
        )
        user = userV.getOrElse(throw new RuntimeException())

        auth <- module.users.authenticate(user.email, PlainText("WRONG"))
      } yield auth

      val auth = Await.result(authF, 2.seconds)
      auth should equal (None)
    }
  }

  "A UserService" should "allow users to log in and out" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val tokenF = for {
        userV <- module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass")
        )
        user = userV.getOrElse(throw new RuntimeException())

        tokenO <- module.users.login(user.email, PlainText("pass"))
      } yield tokenO.get

      val token = Await.result(tokenF, 2.seconds)

      val authF = module.users.authenticate(Email("test@example.com"), token)
      val auth = Await.result(authF, 2.seconds)

      auth should not equal (None)
      auth.get.email should equal (Email("test@example.com"))

      Await.result(
        module.users.logout(Email("test@example.com"), token),
        2.seconds)

      val authAgainF = module.users.authenticate(Email("test@example.com"), token)
      val authAgain = Await.result(authAgainF, 2.seconds)
      authAgain should equal (None)
    }
  }

  "A UserService" should "allow users to log in with other user's tokens" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val tokenF = for {
        userV <- module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass")
        )
        user = userV.getOrElse(throw new RuntimeException())

        tokenO <- module.users.login(user.email, PlainText("pass"))
      } yield tokenO.get

      val token = Await.result(tokenF, 2.seconds)

      val authF = module.users.authenticate(Email("not-test@example.com"), token)
      val auth = Await.result(authF, 2.seconds)

      auth should equal (None)
    }
  }

  private def withUsersModule(f: UsersModule[Future] with ActorSystemModule => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new ActorUsersModule with ActorSystemModule {
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

