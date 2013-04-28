package org.freeclimbers.api.routes

import spray.http._
import spray.http.HttpHeaders._
import spray.routing.HttpService

import org.freeclimbers.api.{Marshalling,JsonProtocols,PageLimits}

trait ApiRoutes extends HttpService
                with Marshalling
                with JsonProtocols {

  protected val TODO = complete(HttpResponse(StatusCodes.NotImplemented))

  protected val pageLimitParams = parameters(
    'limit.as[Long] ? 100L,
    'offset.as[Long] ? 0L).as(PageLimits)
}
