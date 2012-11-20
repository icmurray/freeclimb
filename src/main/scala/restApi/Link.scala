package freeclimb.restApi

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

case class Link(val href: String)

object Link extends LinkJsonFormats {
  def get(crag: Crag) = Link("/crags/" + crag.name)
  def get(climb: Climb) = Link("/climbs/" + climb.crag.name + "/" + climb.name)
  def climbs(crag: Crag) = Link("/climbs/" + crag.name)
  val crags = Link("/crags")
}

trait LinkJsonFormats {

  implicit object LinkJsonFormat extends JsonFormat[Link] {
    def write(link: Link) = Map("href" -> link.href).toJson
    def read(value: JsValue) = deserializationError("Not implemented")
  }

}
