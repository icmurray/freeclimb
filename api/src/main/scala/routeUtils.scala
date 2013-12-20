package org.freeclimbers.api

import spray.http.StatusCode
import spray.routing.Directives

trait RouteUtils {
  this: Directives =>

  def mapSuccessStatusTo(status: StatusCode) = {
    mapHttpResponse { response =>
      response.status.isSuccess match {
        case true  => response.copy(status=status)
        case false => response
      }
    }
  }
}
