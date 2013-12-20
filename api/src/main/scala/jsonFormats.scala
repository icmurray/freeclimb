package org.freeclimbers.api

import java.util.UUID

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import org.freeclimbers.core.{CragId, ClimbId}

trait SupportJsonFormats extends UtilFormats
                            with ModelFormats

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

trait ModelFormats {
  this: UtilFormats =>

  implicit val climbIdFormat = new JsonFormat[ClimbId] {
    def write(id: ClimbId) = JsString(id.uuid.toString)
    def read(js: JsValue) = ClimbId(js.convertTo[UUID])
  }

  implicit val cragIdFormat = new JsonFormat[CragId] {
    def write(id: CragId) = JsString(id.uuid.toString)
    def read(js: JsValue) = CragId(js.convertTo[UUID])
  }

}
