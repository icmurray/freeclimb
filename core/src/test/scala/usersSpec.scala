package org.freeclimbers.core

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import scalaz._
import scalaz.Id._

class UserServiceSpec extends FlatSpec with ShouldMatchers {
  "A UserService" should "register new users" in {
    val module = newUsersModule()
    val user = module.users.register(
      Email("test@example.com"), "Test", "User", PlainText("pass"))
    user.isSuccess should equal (true)
  }

  "A UserService" should "not re-register an existing user" in {
    val module = newUsersModule()
    val user = module.users.register(
      Email("test@example.com"), "Test", "User", PlainText("pass"))
    user.isSuccess should equal (true)

    val sameUser = module.users.register(
      Email("test@example.com"), "Test", "User", PlainText("pass"))
    sameUser.isSuccess should equal(false)
  }

  private def newUsersModule() = new TestUsersModule {}

  trait TestUsersModule extends InMemoryUsersModule[Id] {
    val M = Monad[Id]
  }

}
