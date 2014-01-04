package org.freeclimbers.core

import scala.concurrent.Future

import akka.actor.{ActorSystem}

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import scalaz._
import scalaz.Id._
import scalaz.contrib.std.scalaFuture._
import scalaz.contrib.std.scalaFuture

class UserServiceSpec extends FlatSpec with ShouldMatchers
                                       with TestUtils {

  "A UserService" should "register new users" in {
    withUsersModule { module =>
      val user = blockFor {
        module.users.register(
          Email("test@example.com"), "Test", "User", PlainText("pass")).run
      }
      user.isRight should equal (true)
    }
  }

  "A UserService" should "not re-register an existing user" in {
    withUsersModule { module =>
      implicit val ec = module.ec
      val user = runCommand {
        module.users.register( Email("test@example.com"), "Test", "User", PlainText("pass"))
      }

      val sameUser = blockFor {
        module.users.register(Email(" test@example.com "), "Different", "Name", PlainText("pass"))
      }
      sameUser.isRight should equal(false)
    }
  }

  "A UserService" should "not allow blank emails" in {
    withUsersModule { module =>
      val user = blockFor {
        module.users.register(Email("  "), "Test", "User", PlainText("pass"))
      }
      user.isRight should equal (false)
    }
  }

  "A UserService" should "allow a user to authenticate with password" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val auth = blockFor {
        for {
          userV <- module.users.register(
            Email("test@example.com"), "Test", "User", PlainText("pass")
          ).run
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
          ).run
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
          ).run
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

  "A UserService" should "logout without failure if the session does not exist" in {
    withUsersModule { module =>
      implicit val ec = module.ec

      val token = UserToken.generate()

      blockFor {
        module.users.logout(token)
      }

      val authAgain = blockFor {
        module.users.authenticate(token)
      }
      authAgain should equal (None)
    }
  }

  private def withUsersModule(f: UsersModule[Future] with ActorSystemModule => Unit) = {
    val system = ActorSystem.create("testing", unitTestConfig)
    try {
      val module = new ActorUsersModule with ActorSystemModule {
        implicit def M = scalaFuture.futureInstance
        override lazy val actorSystem = system
      }
      f(module)
    } finally {
      system.shutdown()
    }
  }
}
