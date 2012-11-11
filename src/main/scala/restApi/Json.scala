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
  
  implicit object CragResourceJsonReader
      extends RootJsonReader[Disj[CragResource]] {

    def read(value: JsValue) = {
      implicit val m = value.asJsObject.fields

      extract[JsString]("title").disjunction >>= { title =>
        CragResource(title.value).right
      }
    }

  }

  implicit object RevisionedCragResourceJsonReader
      extends RootJsonReader[Disj[RevisionedCragResource]] {

    def read(value: JsValue) = {
      implicit val m = value.asJsObject.fields
      ( extract[JsString]("title")    |@|
        extract[JsNumber]("revision")
      ).tupled.disjunction >>= { case (title, revision) =>
        RevisionedCragResource(title.value, revision.value.longValue).right
      }
    }

  }

  private def extract[T <: JsValue](field: String)
                                   (implicit m: Map[String, JsValue], M: Manifest[T]) = {
    m.get(field).
      toSuccess("missing value").
      flatMap(tryCast[T](_)).
      enrichAs(field)
  }

  private lazy val jsObjectClass = ClassManifest.fromClass(classOf[JsObject])
  private lazy val jsStringClass = ClassManifest.fromClass(classOf[JsString])
  private lazy val jsNumberClass = ClassManifest.fromClass(classOf[JsNumber])
  private lazy val jsArrayClass = ClassManifest.fromClass(classOf[JsArray])
  private lazy val jsBooleanClass = ClassManifest.fromClass(classOf[JsBoolean])

  private def tryCast[T <: JsValue](o: AnyRef)(implicit m: Manifest[T]) = {
    if (Manifest.singleType(o) <:< m) {
      o.asInstanceOf[T].success
    } else if (m <:< jsObjectClass)  { "Object expected".failure }
      else if (m <:< jsStringClass)  { "String expected".failure }
      else if (m <:< jsNumberClass)  { "Number expected".failure }
      else if (m <:< jsArrayClass)   { "Array expected".failure }
      else if (m <:< jsBooleanClass) { "Boolean expected".failure }
      else                           { "Something else expected".failure }
  }
}

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
