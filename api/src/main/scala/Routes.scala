package org.freeclimbers.api

import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

import org.freeclimbers.core.ClimbServices
import org.freeclimbers.core.ClimbId

trait Routes extends HttpService {

  import Json._

  def service: ClimbServices

  val routes = {
    pathPrefix("climbs") {
      path("") {
        get {
          parameters('from.as[Int] ? 0,
                     'to.as[Int] ? 20) { (from, to) =>
            complete(service.listClimbs(from, to))
          }
        } ~
        post {
          service.createClimb(???, ???, ???)
          ???
        }
      } ~
      pathPrefix(JavaUUID) { uuid =>
        val id = ClimbId(uuid)
        path("") {
          get {
            service.getClimb(ClimbId(uuid))
            ???
          } ~
          put {
            service.updateClimb(ClimbId(uuid), ???, ???, ???)
            ???
          } ~
          delete {
            service.deleteClimb(id, ???)
            ???
          }
        } ~
        path("moveToCrag") {
          put {
            service.moveClimb(id, ???, ???)
            ???
          }
        }
      }
    }
  }
}
