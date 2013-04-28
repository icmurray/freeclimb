package org.freeclimbers.api.routes

import org.freeclimbers.api.{PageLimits, PageLinker}
import org.freeclimbers.api.controllers.ClimbControllerComponent

trait ClimbRoutes extends ApiRoutes { this: ClimbControllerComponent =>

  val climbRoutes = {
    pathPrefix("climbs") {
      path ("") {
        get {
          pageLimitParams { paging =>
            complete {
              climbController.getPage(paging, climbsPageLinker)
            }
          }
        }
      } ~
      pathPrefix(LongNumber) { climbId =>
        path ("") {
          get { TODO } ~
          put { TODO }
        } ~
        path ("changeCrag") {
          put { TODO }
        } ~
        path ("changes" / "latest") {
          get { TODO }
        } ~
        path ("changes" / LongNumber) { page =>
          get { TODO }
        }
      }
    }
  }

  val climbsPageLinker = PageLinker { paging =>
    s"/climbs?limit=${paging.limit}&offset=${paging.offset}"
  }

}
