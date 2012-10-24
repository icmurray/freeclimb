package freeclimb.restApi

import scalaz._
import Scalaz._
import scalaz.std

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

object ModelJson extends CragJson
                    with ClimbJson
                    with RevisionedJson

trait CragJson {
  import JsonInstances._

  implicit object CragJsonWriter extends RootJsonWriter[Crag] {
    def write(crag: Crag) = {
      cragRepresentation(crag) |+| links(crag) |+| embedded(crag)
    }

    private def cragRepresentation(crag: Crag) = Map(
      "name"  -> crag.name,
      "title" -> crag.title
    ).toJson.asJsObject

    private def links(crag: Crag) = JsObject("_links" -> Map(
      "self"   -> Link.get(crag),
      "climbs" -> Link.climbs(crag)
    ).toJson.asJsObject)

    private def embedded(crag: Crag) = Map[String, JsValue]().toJson.asJsObject

  }
}

trait ClimbJson { self: CragJson =>
  import JsonInstances._

  implicit object GradeJsonWriter extends RootJsonWriter[Grade] {
    def write(grade: Grade) = Map(
      "system"     -> grade.system.toString,
      "difficulty" -> grade.toString
    ).toJson

  }

  implicit object ClimbJsonWriter extends RootJsonWriter[Climb] {
    def write(climb: Climb) = {
      climbRepresentation(climb) |+| links(climb) |+| embedded(climb)
    }

    def climbRepresentation(climb: Climb) = Map(
      "name"        -> climb.name.toJson,
      "title"       -> climb.title.toJson,
      "description" -> climb.description.toJson,
      "grade"       -> climb.grade.toJson
    ).toJson.asJsObject

    private def links(climb: Climb) = JsObject("_links" -> Map(
      "self"   -> Link.get(climb)
    ).toJson.asJsObject)

    private def embedded(climb: Climb) = JsObject(
      "_embedded" -> crag(climb)
    )

    private def crag(climb: Climb) = Map(
      "crag" -> climb.crag.toJson
    ).toJson.asJsObject

  }
}

trait RevisionedJson {
  import JsonInstances._

  implicit def revisionedJsonFormat[T : RootJsonWriter] = new RootJsonWriter[Revisioned[T]] {
    def write(revision: Revisioned[T]) = {
      val modelJson = implicitly[RootJsonWriter[T]].write(revision.model).asJsObject
      modelJson |+| JsObject("revision" -> JsNumber(revision.revision))
    }
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Not implemented")
    }
  }

}

private object JsonInstances {

  implicit def jsObjectMonoid: Monoid[JsObject] = new Monoid[JsObject] {
    def zero = JsObject()
    def append(o1: JsObject, o2: => JsObject) = JsObject(
      scalaz.std.map.unionWith(o1.fields, o2.fields)((a,b) => a)
    )
  }

}
