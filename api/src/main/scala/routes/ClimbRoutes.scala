package org.freeclimbers.api.routes

import org.freeclimbers.core.dal.ClimbRepository

import org.freeclimbers.api.{PaginationRequest, PagedResponse}

trait ClimbRoutes extends ApiRoutes {

  protected def climbRepo: ClimbRepository

  val climbRoutes = {
    path("climbs") {
      get {
        parameters('limit.as[Long] ? 100L,
                   'offset.as[Long] ? 0L).as(PaginationRequest) { paging =>
          complete {
            for {
              (climbs,count) <- climbRepo.getPage(paging.limit, paging.offset)
            } yield PagedResponse(paging, count, climbs)
          }
        }
      }
    }
  }

}
