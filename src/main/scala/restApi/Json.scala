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

  implicit object CragJsonWriter extends RootJsonWriter[Crag]
                                    with HalJson[Crag] {

    override protected def modelRepr(crag: Crag) = Map(
      "name"  -> crag.name,
      "title" -> crag.title
    ).toJson.asJsObject

    override protected def links(crag: Crag) = Map(
      "self"   -> Link.get(crag),
      "climbs" -> Link.climbs(crag)
    ).toJson.asJsObject

    override protected def embedded(crag: Crag) = Map[String, JsValue]().toJson.asJsObject

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

  implicit object ClimbJsonWriter extends RootJsonWriter[Climb]
                                     with HalJson[Climb]{

    override protected def modelRepr(climb: Climb) = Map(
      "name"        -> climb.name.toJson,
      "title"       -> climb.title.toJson,
      "description" -> climb.description.toJson,
      "grade"       -> climb.grade.toJson
    ).toJson.asJsObject

    override protected def links(climb: Climb) = Map(
      "self"   -> Link.get(climb)
    ).toJson.asJsObject

    override protected def embedded(climb: Climb) = Map(
      "crag" -> climb.crag.toJson
    ).toJson.asJsObject

  }
}

trait HalJson[T] {

  import JsonInstances._

  def write(t: T) = modelRepr(t) |+| linksRepr(t) |+| embeddedRepr(t)

  protected def linksRepr(t: T) = JsObject("_links" -> links(t))
  protected def embeddedRepr(t: T) = JsObject("_embedded" -> embedded(t))

  protected def modelRepr(t: T): JsObject
  protected def links(t: T): JsObject
  protected def embedded(t: T): JsObject
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
