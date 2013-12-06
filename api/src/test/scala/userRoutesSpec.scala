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
        .returning(newUser(email, firstName, lastName, plaintext))

      Post("/user", json) ~> module.userRoutes ~> check {
        status should equal (StatusCodes.Created)
        val user = responseAs[JsObject]
        user.fields("email") should equal (JsString(email.s))
        user.fields.get("id") should not equal (None)
        user.fields.get("password") should equal (None)
      }
    }
  }

  "/users" should "respond with bad request when user already exists" in {
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

  "/users" should "respond with bad request when validation fails" in {
    withUsersEndpoint { module =>

      val email = Email("test@example.com")
      val firstName = ""              // invalid
      val lastName = "User"
      val plaintext = PlainText("")   // invalid

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
        .returning(List("first_name cannot be empty",
                        "password cannot be empty").failure)

      Post("/user", json) ~> module.userRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
        responseAs[String] should include ("first_name cannot be empty")
        responseAs[String] should include ("password cannot be empty")
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
                      password: PlainText) = {
    User(UserId.createRandom(), email,
         firstName, lastName, Digest(password.s)).success
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withUsersEndpoint(f: UserRoutes[Id] with UsersModule[Id] => Unit) = {

    val module = new UserRoutes[Id] with UsersModule[Id] with IdMarshalling {
      override val users = mock[UserService]
      implicit def M = id
    }

    f(module)
  }

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] = {
    Unmarshaller.delegate[String, JsObject](MediaTypes.`application/json`) { string =>
      string.asJson.asJsObject
    }
  }
}
