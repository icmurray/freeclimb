package org.freeclimbers.api.routes

import org.freeclimbers.api.PaginationRequest

trait ClimbRoutes extends ApiRoutes {

  val climbRoutes = {
    path("climbs") {
      get {
        parameters('limit.as[Long] ? 100L,
                   'offset.as[Long] ? 0L).as(PaginationRequest) { paging =>
          TODO
        }
      }
    }
  }

}
