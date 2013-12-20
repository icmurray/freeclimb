package org.freeclimbers.api

import java.util.UUID

import spray.http.StatusCode
import spray.routing.Directives

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

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

trait UtilFormats {
  implicit val uuidJsonFormat = new JsonFormat[UUID] {
    def read(json: JsValue) = {
      try {
        json match {
          case JsString(s) => UUID.fromString(s)
        }
      } catch {
        case t: Throwable => throw new DeserializationException("UUID expected")
      }
    }

    def write(uuid: UUID) = {
      JsString(uuid.toString)
    }
  }
}

