package freeclimb.restApi

import spray.httpx.marshalling._
import spray.http.MediaTypes._
import spray.json._

import freeclimb.models._
import freeclimb.restApi.ModelJson._

class BasicModelMarshallers(val prettyPrint: Boolean) extends ModelMarshallers {
  override lazy val printer = if (prettyPrint) { PrettyPrinter } else { CompactPrinter }
}

trait ModelMarshallers {

  def printer: JsonPrinter

  val `application/hal+json` = register(new ApplicationMediaType("hal+json", "json"))

  implicit val cragMarshaller = modelMarshaller[Crag]
  implicit val revisionedCragMarshaller = modelMarshaller[Revisioned[Crag]]
  implicit val climbMarshaller = modelMarshaller[Climb]
  implicit val revisionedClimbMarshaller = modelMarshaller[Revisioned[Climb]]

  private def modelMarshaller[T : RootJsonWriter]: Marshaller[T] =
    Marshaller.delegate[T, String](`application/json`, `application/hal+json`) { value =>
      printer(value.toJson)
    }

}
