package org.freeclimbers.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport

import spray.http.{StatusCode, StatusCodes, ContentTypes}
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.routing.Directives

import org.freeclimbers.core.{UsersModule, Email, PlainText, User}
import org.freeclimbers.core.{Validated, DomainError}

trait UtilMarshallers {

  implicit val domainErrorJson = new RootJsonWriter[DomainError] {
    def write(e: DomainError) = JsObject(
      "errors" -> JsArray(e.map(msg => JsString(msg)))
    )
  }

  implicit def validationResponseMarshaller[T](implicit mT: ToResponseMarshaller[T])
                 : ToResponseMarshaller[Validated[T]] = {

    ToResponseMarshaller.of[Validated[T]](ContentTypes.`application/json`) {
        (value, contentType, ctx) =>
      value match {
        case Success(t) => mT(t, ctx)
        case Failure(e) =>
          implicit val deMarshaller = SprayJsonSupport.sprayJsonMarshaller[DomainError](domainErrorJson)
          val domainErrorResponseMarshaller = ToResponseMarshaller.fromMarshaller(
            status=StatusCodes.BadRequest)(deMarshaller)
          domainErrorResponseMarshaller(e, ctx)
      }
    }
  }

}

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
                           with UtilMarshallers {
  this: UsersModule[M] =>

  import spray.httpx.SprayJsonSupport._

  implicit def MarshallerM[T](implicit m: Marshaller[T]): Marshaller[M[T]]
  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]): ToResponseMarshaller[M[T]]

  lazy val userRoutes = {
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
    }
  }

  private def mapSuccessStatusTo(status: StatusCode) = {
    mapHttpResponse { response =>
      response.status.isSuccess match {
        case true  => response.copy(status=status)
        case false => response
      }
    }
  }

}

