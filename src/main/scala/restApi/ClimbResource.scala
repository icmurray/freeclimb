package freeclimb.restApi

import scalaz._
import Scalaz._

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

trait ClimbLinks {
  def climb: Climb

  lazy val links = Map("self" -> Link.get(climb))
}

case class ClimbResource(val climb: Climb) extends Resource[Climb] with ClimbLinks {
  lazy val resource = climb
}
case class RevisionedClimbResource(revision: Revisioned[Climb]) extends Resource[Revisioned[Climb]] with ClimbLinks {
  lazy val climb = revision.model
  lazy val resource = revision
}

object ClimbResource extends ClimbResourceJsonFormats
object RevisionedClimbResource extends ClimbResourceJsonFormats

trait ClimbResourceJsonFormats extends ResourceJsonFormats {

  implicit object ClimbJsonFormat extends RootJsonFormat[Climb] {
    def write(climb: Climb) = Map(
      "name"        -> climb.name,
      "title"       -> climb.title,
      "description" -> climb.description,
      "grade"       -> climb.grade.toString   // TODO: JsonFormat for Grade
    ).toJson
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Climb Expected")
    }
  }

  //implicit object ClimbResourceJsonFormat extends RootJsonWriter[ClimbResource] {
  //  def write(resource: ClimbResource) = {
  //    import CragResource._
  //    val resourceJson = resource.climb.toJson.asJsObject
  //    val linksJson = JsObject("_links" -> resource.links.toJson)
  //    val crag = CragResource(resource.climb.crag).toJson
  //    val embeddedJson = JsObject("_embedded" -> crag)

  //    resourceJson |+| linksJson |+| embeddedJson
  //  }
  //}
  //
  //implicit object RevisionedClimbResourceJsonFormat extends RootJsonWriter[RevisionedClimbResource] {
  //  def write(resource: RevisionedClimbResource) = {
  //    import RevisionedCragResource._
  //    val resourceJson = resource.revision.toJson.asJsObject
  //    val linksJson = JsObject("_links" -> resource.links.toJson)
  //    val crag = CragResource(resource.climb.crag).toJson
  //    val embeddedJson = JsObject("_embedded" -> crag)

  //    resourceJson |+| linksJson |+| embeddedJson
  //  }
  //}
}

