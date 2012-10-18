package freeclimb.restApi

import scalaz._
import Scalaz._

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

trait CragLinks {

  def crag: Crag

  lazy val links = Map(
    "self"   -> Link.get(crag),
    "climbs" -> Link.climbs(crag)
  )
}

case class CragResource(val crag: Crag) extends Resource[Crag] with CragLinks {
  lazy val resource = crag
}
case class RevisionedCragResource(revision: Revisioned[Crag]) extends Resource[Revisioned[Crag]] with CragLinks {
  lazy val crag = revision.model
  lazy val resource = revision
}

object CragResource extends CragResourceJsonFormats
object RevisionedCragResource extends CragResourceJsonFormats

trait CragResourceJsonFormats extends ResourceJsonFormats {

  implicit object CragJsonFormat extends RootJsonFormat[Crag] {
    def write(crag: Crag) = Map(
      "name"  -> crag.name,
      "title" -> crag.title
    ).toJson
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Crag Expected")
    }
  }

  //implicit object CragResourceJsonFormat extends RootJsonWriter[CragResource] {
  //  def write(resource: CragResource) = {
  //    resource.crag.toJson.asJsObject |+| JsObject("_links" -> resource.links.toJson)
  //  }
  //}

  //implicit object RevisionedCragResourceJsonFormat extends RootJsonWriter[RevisionedCragResource] {
  //  def write(resource: RevisionedCragResource) = {
  //    resource.revision.toJson.asJsObject |+| JsObject("_links" -> resource.links.toJson)
  //  }
  //}
}
