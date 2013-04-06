package org.freeclimbers.api.routes

import spray.http._
import spray.httpx.SprayJsonSupport
import spray.http.HttpHeaders._
import spray.routing.HttpService

import org.freeclimbers.api.JsonProtocols

trait ApiRoutes extends HttpService
                with SprayJsonSupport
                with JsonProtocols {

  protected val TODO = complete(HttpResponse(StatusCodes.NotImplemented))

}
