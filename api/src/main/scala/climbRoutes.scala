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

import org.freeclimbers.core.{Climb, ClimbId, RoutesDatabaseModule, CragId}

case class ClimbCreation(
    name: String,
    description: String,
    cragUUID: UUID)

object ClimbCreation extends UtilFormats {
  implicit val asJson = jsonFormat(ClimbCreation.apply _, "name", "description", "crag_id")
}

case class ClimbResource(
    id: ClimbId,
    name: String,
    description: String,
    crag: CragLink)

object ClimbResource extends SupportJsonFormats {
  def apply(climb: Climb): ClimbResource = {
    ClimbResource(climb.id, climb.name, climb.description, CragLink(climb.cragId))
  }
  implicit val asJson = jsonFormat(ClimbResource.apply _, "id", "name", "description", "crag")
}

case class ListingResource[T](
    results: Seq[T],
    total: Int)

object ListingResource {
  def fromSeq[T](ts: Seq[T]): ListingResource[T] = {
    ListingResource(ts, ts.length)
  }

  implicit def asJson[T : JsonFormat] = jsonFormat(ListingResource.apply[T] _, "results", "total")
}

trait ClimbRoutes[M[+_]] extends Directives
                            with UtilFormats
                            with RouteUtils
                            with MarshallingUtils {
  this: RoutesDatabaseModule[M] with HigherKindedUtils[M] =>

  implicit private val climbIdJsonFormat = jsonFormat(ClimbId.apply _, "id")

  def climbRoutes = {
    path("climbs") {
      post {
        entity(as[ClimbCreation]) { climb =>
          mapSuccessStatusTo(StatusCodes.Created) {
            complete {
              routesDB.createClimb(climb.name, climb.description, CragId(climb.cragUUID)).run
            }
          }
        }
      } ~
      get {
        complete {
          routesDB.climbs.map(_.map(ClimbResource(_))).map(ListingResource.fromSeq)
        }
      }
    } ~
    path("climbs" / JavaUUID) { uuid =>
      val id = ClimbId(uuid)
      get {
        rejectEmptyResponse {
          complete {
            routesDB.climbById(id).map(_.map(ClimbResource.apply))
          }
        } ~ rejectEmptyResponse { ctx =>
          routesDB.resolveClimb(id).map {
            case None => ctx.complete(None: Option[ClimbResource])
            case Some(climb) => ctx.redirect("/climbs/" + climb.id.uuid.toString,
                                             StatusCodes.MovedPermanently)
          }
        }
      }
    }
  }
}
