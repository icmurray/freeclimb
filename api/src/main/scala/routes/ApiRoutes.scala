package org.freeclimbers.api.routes

import spray.http._
import spray.http.HttpHeaders._
import spray.routing.HttpService

import org.freeclimbers.api.{Marshalling,JsonProtocols}

trait ApiRoutes extends HttpService
                with Marshalling
                with JsonProtocols {

  protected val TODO = complete(HttpResponse(StatusCodes.NotImplemented))

}
