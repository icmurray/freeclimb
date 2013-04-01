package org.freeclimbers.api.routes

import spray.http._
import spray.http.HttpHeaders._
import spray.routing.HttpService

trait ClimbRoutes extends HttpService {

  private val TODO = complete(HttpResponse(StatusCodes.NotImplemented))

  val climbRoutes = {
    path("climbs") {
      get {
        TODO
      }
    }
  }

}
