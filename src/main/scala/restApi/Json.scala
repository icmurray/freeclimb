package freeclimb.restApi

import scalaz._
import Scalaz._
import scalaz.std

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._
import freeclimb.validation._

object ModelJson extends CragJson
                    with ClimbJson
                    with RevisionedJson
                    with ResourceJson

trait ResourceJson {
  
  implicit object CragResourceJsonReader extends RootJsonReader[\/[Map[String,NonEmptyList[String]], CragResource]] {

    def read(value: JsValue) = {
      val m = value.asJsObject.fields
      ( m.get("name")  .toSuccess("missing value").enrichAs("name")  |@|
        m.get("title") .toSuccess("missing value").enrichAs("title")
        ).tupled.disjunction >>= {
        // TODO: fail nicely if two JsStrings are not found
        case (JsString(name), JsString(title)) => CragResource(title).right
        case _                                 => Map("something" -> NonEmptyList("Failed")).left
      }
    }

  }

  implicit object RevisionedCragResourceJsonReader extends RootJsonReader[\/[Map[String,NonEmptyList[String]], RevisionedCragResource]] {

    def read(value: JsValue) = {
      val m = value.asJsObject.fields
      ( m.get("name")     .toSuccess("missing value").enrichAs("name")    |@|
        m.get("title")    .toSuccess("missing value").enrichAs("title")   |@|
        m.get("revision") .toSuccess("missing value").enrichAs("revision")
      ).tupled.disjunction >>= {
        // TODO: fail nicely if two JsStrings are not found
        case (JsString(name), JsString(title), JsNumber(revision)) => RevisionedCragResource(title, revision.longValue).right
        case _                                 => Map("something" -> NonEmptyList("Failed")).left
      }
    }

  }
}

trait CragJson {
  import JsonInstances._

  implicit object RevisionedCragJsonRead extends RootJsonReader[\/[Map[String, NonEmptyList[String]], Revisioned[Crag]]] {
    def read(value: JsValue) = {
      val m = value.asJsObject.fields
      (
        m.get("name")     .toSuccess("missing value").enrichAs("name")    |@|
        m.get("title")    .toSuccess("missing value").enrichAs("title")   |@|
        m.get("revision") .toSuccess("missing value").enrichAs("revision")
      ).tupled.disjunction >>= {
        case (JsString(name), JsString(title), JsNumber(revision)) => Crag(name, title) map { c => Revisioned(revision.longValue, c) }
        case _                                                     => Map("something" -> NonEmptyList("Failed")).left
      }
    }
  }

  implicit object CragJsonReader extends RootJsonReader[\/[Map[String,NonEmptyList[String]], Crag]] {

    def read(value: JsValue) = {
      val m = value.asJsObject.fields
      ( m.get("name")  .toSuccess("missing value").enrichAs("name")  |@|
        m.get("title") .toSuccess("missing value").enrichAs("title")
        ).tupled.disjunction >>= {
        // TODO: fail nicely if two JsStrings are not found
        case (JsString(name), JsString(title)) => Crag(name, title)
        case _                                 => Map("something" -> NonEmptyList("Failed")).left
      }
    }

  }
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

  implicit def revisionedJsonWriter[T : RootJsonWriter] = new RootJsonWriter[Revisioned[T]] {
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
