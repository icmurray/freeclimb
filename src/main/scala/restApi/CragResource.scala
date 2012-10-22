package freeclimb.restApi

import scalaz._
import Scalaz._

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

object CragResource extends CragResourceJsonFormats {
  def apply(c: Crag): Resource[Crag] = new Resource[Crag] with CragRelations {
    override lazy val crag = c
    override lazy val resource = crag
  }
}

object RevisionedCragResource extends CragResourceJsonFormats {

  // Locally used type synonym for brevity
  private type RRCrag = Resource[Revisioned[Crag]]

  def apply(revision: Revisioned[Crag]): RRCrag = new RRCrag with CragRelations {
    override lazy val crag = revision.model
    override lazy val resource = revision
  }
}

trait CragResourceJsonFormats extends ResourceJsonFormats {

  implicit object CragJsonFormat extends RootJsonFormat[Crag] {
    def write(crag: Crag) = Map(
      "name"  -> crag.name,
      "title" -> crag.title
    ).toJson
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Not implemented: CragJsonFormat.read()")
    }
  }
}

private trait CragRelations {
  def crag: Crag

  lazy val links = Map(
    "self"   -> Link.get(crag),
    "climbs" -> Link.climbs(crag)
  )
  
  lazy val embedded = Map[String, JsValue]()
}

