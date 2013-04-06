package org.freeclimbers.api.routes

import org.freeclimbers.api.PaginationRequest
import org.freeclimbers.api.controllers.ClimbControllerComponent

trait ClimbRoutes extends ApiRoutes { this: ClimbControllerComponent =>

  val climbRoutes = {
    path("climbs") {
      get {
        parameters('limit.as[Long] ? 100L,
                   'offset.as[Long] ? 0L).as(PaginationRequest) { paging =>
          complete {
            climbController.getPage(paging)
          }
        }
      }
    }
  }

}
