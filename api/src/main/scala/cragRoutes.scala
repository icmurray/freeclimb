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

import org.freeclimbers.core.{Crag, CragId, RoutesDatabaseModule}

case class CragCreation(
    name: String,
    description: String)

object CragCreation extends UtilFormats {
  implicit val asJson = jsonFormat(CragCreation.apply _, "name", "description")
}

case class CragResource(id: String, name: String, description: String, climbs: CragClimbsListingLink)
object CragResource {
  def apply(crag: Crag): CragResource = {
    CragResource(crag.id.uuid.toString, crag.name, crag.description, CragClimbsListingLink.ofCrag(crag.id))
  }
  implicit val asJson = jsonFormat(CragResource.apply _, "id", "name", "description", "climbs")
}

trait CragRoutes[M[+_]] extends Directives
                           with UtilFormats
                           with RouteUtils
                           with MarshallingUtils {
  this: RoutesDatabaseModule[M] with HigherKindedUtils[M] =>

  implicit private val cragIdJsonFormat = jsonFormat(CragId.apply _, "id")

  def cragRoutes = {
    path("crags") {
      post {
        entity(as[CragCreation]) { crag =>
          mapSuccessStatusTo(StatusCodes.Created) {
            complete {
              routesDB.createCrag(crag.name, crag.description).run
            }
          }
        }
      } ~
      get {
        complete {
          routesDB.crags.map(_.map(CragResource(_))).map(ListingResource.fromSeq)
        }
      }
    } ~
    path("crags" / JavaUUID) { uuid =>
      val id = CragId(uuid)
      get {
        complete {
          routesDB.cragById(id).map(_.map(CragResource.apply))
        }
      }
    }
  }
}
