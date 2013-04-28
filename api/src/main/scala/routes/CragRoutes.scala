package org.freeclimbers.api.routes

import org.freeclimbers.api.{PageLimits, PageLinker}
//import org.freeclimbers.api.controllers.CragControllerComponent

trait CragRoutes extends ApiRoutes {
//trait CragRoutes extends ApiRoutes { this: CragControllerComponent =>

  val cragRoutes = {
    pathPrefix("crags") {
      path("") {
        get {
          parameters('limit.as[Long] ? 100L, 'offset.as[Long] ? 0L).as(PageLimits) { paging =>
            //complete {
              //cragController.getPage(paging, cragsPageLinker)
              TODO
            //}
          }
        }
      } ~
      pathPrefix(LongNumber) { cragId =>
        path("") {
          get { TODO } ~
          put { TODO }
        } ~
        path ("changes" / "latest") {
          get { TODO }
        } ~
        path ("changes" / LongNumber) { page =>
          get { TODO }
        } ~
        path ("climbs") {
          get { TODO } ~
          post { TODO }
        }
      }
    }
  }

  val cragsPageLinker = PageLinker { paging =>
    s"/crags?limit=${paging.limit}&offset=${paging.offset}"
  }

}
