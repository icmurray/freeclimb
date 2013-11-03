package org.freeclimbers.api

import java.util.UUID

import spray.json._
import DefaultJsonProtocol._ 

import org.freeclimbers.core.{ClimbId,CragId}
import org.freeclimbers.core.queries.Climb

object Json extends DefaultJsonProtocol {

  implicit val uuidWriter = new JsonFormat[UUID] {
    override def write(value: UUID) = JsString(value.toString)
    override def read(js: JsValue) = ???
  }

  implicit val climbIdFormat = jsonFormat1(ClimbId.apply)
  implicit val cragIdFormat = jsonFormat1(CragId.apply)
  implicit val climbFormat = jsonFormat4(Climb)
}
