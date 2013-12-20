package org.freeclimbers
package api

import java.util.UUID

import scala.concurrent.{Future, ExecutionContext, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import spray.routing.{Directives, RejectionHandler, Rejection}
import spray.http.StatusCodes

import org.freeclimbers.core.{Climb, ClimbId, ClimbsModule, CragId}

trait UtilFormats {
  implicit val uuidJsonFormat = new JsonFormat[UUID] {
    def read(json: JsValue) = {
      try {
        json match {
          case JsString(s) => UUID.fromString(s)
        }
      } catch {
        case t: Throwable => throw new DeserializationException("UUID expected")
      }
    }

    def write(uuid: UUID) = {
      JsString(uuid.toString)
    }
  }
}

case class ClimbCreation(
    name: String,
    description: String,
    cragUUID: UUID)

object ClimbCreation extends UtilFormats {
  implicit val asJson = jsonFormat(ClimbCreation.apply _, "name", "description", "crag_id")
}

case class ClimbResource(id: String, name: String)
object ClimbResource {
  def apply(climb: Climb): ClimbResource = ClimbResource(climb.id.uuid.toString, climb.name)
  implicit val asJson = jsonFormat(ClimbResource.apply _, "id", "name")
}

trait ClimbRoutes[M[+_]] extends Directives
                            with UtilFormats
                            with RouteUtils
                            with MarshallingUtils {
  this: ClimbsModule[M] with HigherKindedUtils[M] =>

  implicit private val climbIdJsonFormat = jsonFormat(ClimbId.apply _, "id")

  def climbRoutes = {
    path("climbs") {
      post {
        entity(as[ClimbCreation]) { climb =>
          mapSuccessStatusTo(StatusCodes.Created) {
            complete {
              climbs.create(climb.name, climb.description, CragId(climb.cragUUID))
            }
          }
        }
      }
    } ~
    path("climbs" / JavaUUID) { uuid =>
      val id = ClimbId(uuid)
      get {
        rejectEmptyResponse {
          complete {
            climbs.withId(id).map(_.map(ClimbResource.apply))
          }
        } ~ rejectEmptyResponse { ctx =>
          climbs.resolvesTo(id).map {
            case None => ctx.complete(None: Option[ClimbResource])
            case Some(climb) => ctx.redirect("/climbs/" + climb.id.uuid.toString,
                                             StatusCodes.MovedPermanently)
          }
        }
      }
    }
  }
}
