package org.freeclimbers.api.routes

import spray.http._
import spray.http.HttpHeaders._
import spray.routing.HttpService

trait ApiRoutes extends HttpService {

  protected val TODO = complete(HttpResponse(StatusCodes.NotImplemented))

}
