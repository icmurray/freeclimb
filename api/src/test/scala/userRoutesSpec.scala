package org.freeclimbers.api

import java.io.{FileWriter, File}

import scala.concurrent.{Future, future}

import org.scalamock.scalatest.MockFactory

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.httpx.unmarshalling.Unmarshaller
import ContentTypes._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import scalaz._
import Scalaz._

import org.freeclimbers.core.UsersModule
import org.freeclimbers.core.{Email, PlainText, Digest, User, UserId, UserToken}
import org.freeclimbers.core.{DomainError, Validated}

class UserRoutesSpec extends FlatSpec with ShouldMatchers
                                      with ScalatestRouteTest
                                      with MockFactory {

  override val suiteName = "User API"

  val wikiPage = new File("users.md")
  wikiPage.delete()

  private def writeToWiki(request: HttpRequest,
                          response: HttpResponse,
                          file: File): Unit = {
    file.synchronized {
      val w = new FileWriter(file, true)
      try {
        w.write("# " + suiteName + "\n")
        w.write("## POST " + request.uri.path + "\n\n")
        w.write("HEADERS:\n")
        request.headers.foreach(h => w.write(h.toString))
        w.write("\n```json\n")
        w.write(request.entity.asString)
        w.write("\n```\n")
        response.headers.foreach(h => w.write(h.toString))
        w.write("\n```json\n")
        w.write(response.entity.asString)
        w.write("\n```\n```")
      } finally {
        w.close()
      }
    }
  }

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
        .returning(CResult(newUser(email, firstName, lastName, plaintext).right))

      val request = Post("/user", json)
      request ~> module.userRoutes ~> check {

        writeToWiki(request, response, wikiPage)

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
        .returning(CResult(List("User already exists").left))

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
        .returning(CResult(List("first_name cannot be empty",
                               "password cannot be empty").left))

      Post("/user", json) ~> module.userRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
        responseAs[String] should include ("first_name cannot be empty")
        responseAs[String] should include ("password cannot be empty")
      }
    }
  }

  "POST to /sessions" should "create a new user session" in {
    withUsersEndpoint { module =>

      val userToken = UserToken.generate()
      val email = Email("test@example.com")
      val plaintext = PlainText("password")

      (module.users.login(_: Email, _: PlainText))
        .expects(email, plaintext)
        .returning(Some(userToken))

      val credentials = BasicHttpCredentials("test@example.com", "password")

      Post("/sessions") ~> 
        addCredentials(credentials) ~>
        module.userRoutes ~>
        check {
          status should equal (StatusCodes.Created)
          responseAs[String] should include (userToken.uuid.toString)
        }
    }
  }

  "POST to /sessions" should "fail if no auth credentials provided" in {
    withUsersEndpoint { module =>
      Post("/sessions") ~> 
        module.userRoutes ~>
        check {
          status should equal (StatusCodes.Unauthorized)
        }
    }
  }

  "POST to /sessions" should "fail if incorrect credentials are provided" in {
    withUsersEndpoint { module =>

      val userToken = UserToken.generate()
      val email = Email("test@example.com")
      val plaintext = PlainText("password")

      (module.users.login(_: Email, _: PlainText))
        .expects(email, plaintext)
        .returning(None)

      val credentials = BasicHttpCredentials("test@example.com", "password")

      Post("/sessions") ~> 
        addCredentials(credentials) ~>
        module.userRoutes ~>
        check {
          status should equal (StatusCodes.Unauthorized)
        }
    }
  }

  "DELETE to /sessions/<uuid>" should "delete the given session" in {
    withUsersEndpoint { module =>

      val userToken = UserToken.generate()

      // Not needed ... knowledge of the token is sufficient.
      //(module.users.authenticate(_: UserToken))
      //  .expects(userToken)
      //  .returning(Some(newUser(email, "A Test", "User", plaintext)))

      (module.users.logout _)
        .expects(userToken)

      //val credentials = GenericHttpCredentials("Token", userToken.uuid.toString)

      Delete("/sessions/" + userToken.uuid.toString) ~> 
        //addCredentials(credentials) ~>
        module.userRoutes ~>
        check {
          status should equal (StatusCodes.OK)
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
         firstName, lastName, Digest(password.s))
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withUsersEndpoint(f: UserRoutes[Id] with UsersModule[Id] => Unit) = {

    val module = new UserRoutes[Id] with UsersModule[Id] with IdUtils with HttpService {
      def actorRefFactory = system
      override def userRoutes = sealRoute(super.userRoutes)
      override val users = mock[UserService]
      implicit def M = id
    }

    f(module)
  }

  private[this] def CResult[T](t: Validated[T]) = EitherT[Id, DomainError, T](t)

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] = {
    Unmarshaller.delegate[String, JsObject](MediaTypes.`application/json`) { string =>
      string.asJson.asJsObject
    }
  }
}
