package org.freeclimbers.api

import spray.json._

import org.freeclimbers.core.models._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val climbFormat = jsonFormat1(Climb)
  implicit def pagedResponseFormat[T:JsonFormat] = jsonFormat3(Page[T])
}
