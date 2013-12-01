package org.freeclimbers.api

import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.httpx.unmarshalling.Unmarshaller
import ContentTypes._
import spray.testkit.ScalatestRouteTest

import scalaz._
import Scalaz._

import org.freeclimbers.core.UsersModule
import org.freeclimbers.core.{Email, PlainText, Digest, User, UserId}

class UserRoutesSpec extends FlatSpec with ShouldMatchers
                                      with ScalatestRouteTest
                                      with MockFactory {


  "/users" should "register new users" in {
    withUsersEndpoint { module =>

      val email = Email("test@example.com")
      val firstName = "Test"
      val lastName = "User"
      val plaintext = PlainText("supersecret")

      val json = JsonEntity(s"""
        {
          "email": "${email.s}",
          "first_name": "${firstName}",
          "last_name": "${lastName}",
          "password": "${plaintext.s}"
        }
      """)

      (module.users.register _)
        .expects(email, firstName, lastName, plaintext)
        .returning(newUser(email, firstName, lastName, plaintext).success)

      Post("/user", json) ~> module.userRoutes ~> check {
        status should equal (StatusCodes.Created)
        val user = responseAs[JsObject]
        user.fields("email") should equal (JsString(email.s))
        user.fields.get("id") should not equal (None)
        user.fields.get("password") should equal (None)
      }
    }
  }

  "/users" should "respond with conflict when user already exists" in {
    withUsersEndpoint { module =>

      val email = Email("test@example.com")
      val firstName = "Test"
      val lastName = "User"
      val plaintext = PlainText("supersecret")

      val json = JsonEntity(s"""
        {
          "email": "${email.s}",
          "first_name": "${firstName}",
          "last_name": "${lastName}",
          "password": "${plaintext.s}"
        }
      """)

      (module.users.register _)
        .expects(email, firstName, lastName, plaintext)
        .returning(List("User already exists").failure)

      Post("/user", json) ~> module.userRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
        responseAs[String] should include ("User already exists")
      }
    }
  }

  /**
   * Creates a new User from the given details.
   *
   * The UserId is created randomly, and the password is *not* hashed (because
   * BCrypt is intentionally slow).
   */
  private def newUser(email: Email,
                      firstName: String,
                      lastName: String,
                      password: PlainText): User = {
    User(UserId.createRandom(), email,
         firstName, lastName, Digest(password.s))
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withUsersEndpoint(f: UserRoutes[Id] with UsersModule[Id] => Unit) = {

    val module = new UserRoutes[Id] with UsersModule[Id] {
      override val users = mock[UserService]
      implicit def M = id
      implicit def MarshallerM[T](implicit m: Marshaller[T]) = m
      implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = m
    }

    f(module)
  }

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] = {
    Unmarshaller.delegate[String, JsObject](MediaTypes.`application/json`) { string =>
      string.asJson.asJsObject
    }
  }
}
