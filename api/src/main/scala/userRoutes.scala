package org.freeclimbers.api

import java.util.UUID

import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import spray.http.StatusCodes
import spray.routing.Directives
import spray.routing.authentication.{BasicAuth, UserPass}

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
  this: UsersModule[M] with HigherKindedMarshalling[M] =>

  implicit private val uuidJsonFormat = new JsonFormat[UUID] {
    def read(json: JsValue) = ???
    def write(uuid: UUID) = {
      JsString(uuid.toString)
    }
  }

  implicit private val userTokenJsonFormat = jsonFormat(UserToken.apply _, "id")

  def readM[T](t: M[T]): Future[T]

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
          "TODO"
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
}

