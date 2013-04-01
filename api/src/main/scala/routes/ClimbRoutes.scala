package org.freeclimbers.api.routes

import spray.routing.HttpService

trait ClimbRoutes extends HttpService {

  val climbRoutes = {
    path("climbs") {
      get {
        complete { "OK!" }
      }
    }
  }

}
