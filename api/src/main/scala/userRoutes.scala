package org.freeclimbers.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import spray.json._
import DefaultJsonProtocol._

import spray.routing.Directives

import org.freeclimbers.core.{UsersModule, Email, PlainText}

case class UserRegistration(
    email: String,
    firstName: String,
    lastName: String,
    password: String)

object UserRegistration {

  implicit val asJson = jsonFormat(UserRegistration.apply _,
                                   "email", "first_name", "last_name", "password")
}

trait UserRoutes extends Directives {
  this: UsersModule[Future] =>

  import spray.httpx.SprayJsonSupport._

  lazy val userRoutes = {
    path("user") {
      post {
        entity(as[UserRegistration]) { regDetails =>
          complete {
            users.register(Email(regDetails.email),
                           regDetails.firstName,
                           regDetails.lastName,
                           PlainText(regDetails.password)).map(_.toString)
          }
        }
      }
    }
  }

}

