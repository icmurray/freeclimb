package freeclimb.restApi

import scalaz._
import Scalaz._
import scalaz.std

import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.models._

trait Resource[T] {
  def resource: T
  def links: Map[String, Link]
  def embedded: Map[String, JsValue]
}

trait ResourceJsonFormats extends JsInstances {

  implicit def revisionedResourceJsonFormat[T : RootJsonFormat] = new RootJsonFormat[Revisioned[T]] {
    def write(resource: Revisioned[T]) = {
      val modelJson = implicitly[RootJsonFormat[T]].write(resource.model).asJsObject
      modelJson |+| JsObject("revision" -> JsNumber(resource.revision))
    }
    
    def read(value: JsValue) = value match {
      case _ => deserializationError("Not implemented")
    }
  }

  implicit def resourceJsonWriter[T : RootJsonWriter] = new RootJsonWriter[Resource[T]] {
    def write(resource: Resource[T]) = {
      val resourceJson = implicitly[RootJsonWriter[T]].write(resource.resource).asJsObject
      val linksJson = JsObject("_links" -> resource.links.toJson)
      val embedJson = JsObject("_embedded" -> resource.embedded.toJson)
      resourceJson |+| linksJson |+| embedJson
    }
  }

}

trait JsInstances {

  implicit def jsObjectMonoid: Monoid[JsObject] = new Monoid[JsObject] {
    def zero = JsObject()
    def append(o1: JsObject, o2: => JsObject) = JsObject(
      scalaz.std.map.unionWith(o1.fields, o2.fields)((a,b) => a)
    )
  }

}
