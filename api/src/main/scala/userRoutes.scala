package org.freeclimbers.api

import java.util.UUID

import scala.concurrent.{Future, ExecutionContext, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import spray.http.{StatusCodes, HttpRequest, HttpCredentials, HttpHeader, GenericHttpCredentials}
import spray.routing.{Directives, RequestContext}
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.authentication.{HttpAuthenticator}

import org.freeclimbers.core.{UsersModule, Email, PlainText, User, UserToken}

case class UserRegistration(
    email: String,
    firstName: String,
    lastName: String,
    password: String)

object UserRegistration {
  implicit val asJson = jsonFormat(UserRegistration.apply _,
                                   "email", "first_name", "last_name", "password")
}

case class UserRegistered(
    email: String,
    firstName: String,
    lastName: String,
    id: String)

object UserRegistered {
  def apply(user: User): UserRegistered = {
    UserRegistered(user.email.s,
                   user.firstName,
                   user.lastName,
                   user.id.uuid.toString)
  }

  implicit val asJson = jsonFormat(UserRegistered.apply _,
                                   "email", "first_name", "last_name", "id")
}

trait UserRoutes[M[+_]] extends Directives
                           with RouteUtils
                           with MarshallingUtils {
  this: UsersModule[M] with HigherKindedUtils[M] =>

  implicit private val uuidJsonFormat = new JsonFormat[UUID] {
    def read(json: JsValue) = ???
    def write(uuid: UUID) = {
      JsString(uuid.toString)
    }
  }

  implicit private val userTokenJsonFormat = jsonFormat(UserToken.apply _, "id")

  def userRoutes = {
    path("user") {
      post {
        entity(as[UserRegistration]) { regDetails =>
          mapSuccessStatusTo(StatusCodes.Created) {
            complete {
              users.register(Email(regDetails.email),
                                   regDetails.firstName,
                                   regDetails.lastName,
                                   PlainText(regDetails.password))
                   .map(_.map(UserRegistered(_)))
            }
          }
        }
      }
    } ~ path("sessions") {
      post {
        mapSuccessStatusTo(StatusCodes.Created) {
          performLogin { token =>
            complete { token }
          }
        }
      }
    } ~ path("sessions" / JavaUUID) { sessionId =>
      delete {
        complete {
          val token = UserToken(sessionId)
          users.logout(token).map(_ => "")
        }
      }
    }
  }

  /**
   * Authentication directive for performing a user login.
   *
   * Uses BasicAuth for authentication credentials which are then used to log
   * the user in.  Upon success, a token is returned that can be used to
   * perform future authentication requests.
   */
  private def performLogin = {

    def auth(userPass: Option[UserPass]): Future[Option[UserToken]] = {
      val resultM: M[Option[UserToken]] = userPass match {
        case None     => M.pure(None)
        case Some(up) => users.login(Email(up.user), PlainText(up.pass))
      }
      readM(resultM)
    }

    authenticate(BasicAuth(auth _, realm="secure"))
  }

  //private def tokenAuth = {
  //  authenticate(new TokenHttpAuthenticator())
  //}

  //class TokenHttpAuthenticator(implicit val executionContext: ExecutionContext)
  //    extends HttpAuthenticator[(User, UserToken)] {

  //  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[(User, UserToken)]] = {

  //    val resultM: M[Option[(User,UserToken)]] = credentials match {
  //      case Some(GenericHttpCredentials("Token", tokenString, _)) =>
  //        readUUID(tokenString) match {
  //          case None       => M.pure(None)
  //          case Some(uuid) => users.authenticate(UserToken(uuid)).map(_.map((_, UserToken(uuid))))
  //        }
  //      case _ => M.pure(None)
  //    }
  //    readM(resultM)
  //  }

  //  def getChallengeHeaders(httpRequest: HttpRequest): List[HttpHeader] = Nil

  //  private def readUUID(s: String): Option[UUID] = {
  //    Try { UUID.fromString(s) }.toOption
  //  }

  //}
}

