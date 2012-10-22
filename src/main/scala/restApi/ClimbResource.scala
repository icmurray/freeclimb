package freeclimb.restApi

import scalaz._
import Scalaz._

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

object ClimbResource extends ClimbResourceJsonFormats {
  def apply(c: Climb): Resource[Climb] = new Resource[Climb] with ClimbRelations {
    override lazy val climb = c
    override lazy val resource = climb
  }
}

object RevisionedClimbResource extends ClimbResourceJsonFormats {

  // Locally used type synonym for brevity
  private type RRClimb = Resource[Revisioned[Climb]]

  def apply(revision: Revisioned[Climb]): RRClimb = new RRClimb with ClimbRelations {
     override lazy val climb = revision.model
     override lazy val resource = revision
  }
}

trait ClimbResourceJsonFormats extends ResourceJsonFormats {

  implicit object ClimbJsonFormat extends RootJsonFormat[Climb] {
    def write(climb: Climb) = Map(
      "name"        -> climb.name,
      "title"       -> climb.title,
      "description" -> climb.description,
      "grade"       -> climb.grade.toString   // TODO: JsonFormat for Grade
    ).toJson
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Not implemented: ClimbJsonFormat.read()")
    }
  }
}

private trait ClimbRelations {
  import CragResource._
  def climb: Climb

  lazy val links = Map(
    "self" -> Link.get(climb)
  )

  lazy val embedded = Map(
    "climbs" -> CragResource(climb.crag).toJson
  )
}
