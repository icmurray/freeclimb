package org.freeclimbers.api.routes

import org.freeclimbers.api.{PageLimits, PageLinker}
import org.freeclimbers.api.controllers.ClimbControllerComponent

trait ClimbRoutes extends ApiRoutes { this: ClimbControllerComponent =>

  val climbRoutes = {
    path("climbs") {
      get {
        parameters('limit.as[Long] ? 100L, 'offset.as[Long] ? 0L).as(PageLimits) { paging =>
          complete {
            climbController.getPage(paging, climbsPageLinker)
          }
        }
      }
    }
  }

  val climbsPageLinker = new PageLinker ( paging => s"/climbs?limit=${paging.limit}&offset=${paging.offset}" )

}
