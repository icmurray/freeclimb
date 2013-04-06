package org.freeclimbers.api.routes

import org.freeclimbers.core.controllers.ClimbController

import org.freeclimbers.api.PaginationRequest

trait ClimbRoutes extends ApiRoutes {

  protected def climbController: ClimbController

  val climbRoutes = {
    path("climbs") {
      get {
        parameters('limit.as[Long] ? 100L,
                   'offset.as[Long] ? 0L).as(PaginationRequest) { paging =>
          val (climbs,count) = climbController.getPage(paging.limit, paging.offset)
          TODO
        }
      }
    }
  }

}
