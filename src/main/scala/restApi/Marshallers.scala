package freeclimb.restApi

import scalaz._
import Scalaz._

import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.http._
import spray.http.MediaTypes._
import spray.http.MediaRanges._
import spray.json._
import spray.json.DefaultJsonProtocol._

import freeclimb.api._
import freeclimb.models._
import freeclimb.restApi.ModelJson._

class BasicModelMarshallers(val prettyPrint: Boolean) extends ModelMarshallers
                                                      with RestResourceUnMarshallers
                                                      with MimeTypes {
  override lazy val printer = if (prettyPrint) { PrettyPrinter } else { CompactPrinter }
}

trait MimeTypes {

  val `application/hal+json` = MediaTypes.register(new ApplicationMediaType("hal+json", "json"))

}

trait ModelMarshallers { this: MimeTypes =>

  def printer: JsonPrinter

  implicit val cragMarshaller = modelMarshaller[Crag]
  implicit val revisionedCragMarshaller = modelMarshaller[Revisioned[Crag]]
  implicit val climbMarshaller = modelMarshaller[Climb]
  implicit val revisionedClimbMarshaller = modelMarshaller[Revisioned[Climb]]

  private def modelMarshaller[T : RootJsonWriter]: Marshaller[T] =
    Marshaller.delegate[T, String](`application/json`, `application/hal+json`) { value =>
      printer(value.toJson)
    }
  
  implicit val errorMapMarshaller: Marshaller[Map[String,NonEmptyList[String]]] =
    Marshaller.delegate[Map[String,NonEmptyList[String]], String](`application/json`, `application/hal+json`) { value =>
      printer(value.mapValues(_.toList).toJson)
    }
}
trait RestResourceUnMarshallers { this: MimeTypes =>

  type RichValidation[E,A] = \/[Map[String,NonEmptyList[E]], A]

  implicit val cragUnmarshaller = modelUnmarshaller[RichValidation[String, Crag]]
  implicit val revisionedCragUnMarshaller = modelUnmarshaller[RichValidation[String, Revisioned[Crag]]]

  private def modelUnmarshaller[T : RootJsonReader]: Unmarshaller[T] =
    Unmarshaller.delegate[String, T](`application/json`, `application/hal+json`) { string =>
      string.asJson.convertTo[T]
    }

}
