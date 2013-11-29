package org.freeclimbers.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.http.StatusCodes
import spray.httpx.marshalling.Marshaller
import spray.routing.Directives

import org.freeclimbers.core.{UsersModule, Email, PlainText, User}

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

trait UserRoutes[M[+_]] extends Directives {
  this: UsersModule[M] =>

  import spray.httpx.SprayJsonSupport._

  implicit def MarshallerM[T](implicit m: Marshaller[T]): Marshaller[M[T]]

  lazy val userRoutes = {
    path("user") {
      post {
        entity(as[UserRegistration]) { regDetails =>
          respondWithStatus(StatusCodes.Created) {
            complete {
              users.register(Email(regDetails.email),
                                   regDetails.firstName,
                                   regDetails.lastName,
                                   PlainText(regDetails.password))
                   .map(_.toOption.map(UserRegistered(_)))
            }
          }
        }
      }
    }
  }

}

