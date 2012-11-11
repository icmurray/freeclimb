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
import freeclimb.validation._

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

/**
 * Rather than unmarshalling domain objects directly, there's an extra level
 * of indirection, where resource representations are unmarshalled to
 * representations of the resources, rather than the domain models.  This
 * allows, amongst other things, versioning of the api; and resources to be
 * partial representations of the domain models.
 */
trait RestResourceUnMarshallers { this: MimeTypes =>

  implicit val cragResourceUnmarshaller = modelUnmarshaller[Disj[CragResource]]
  implicit val revisionedCragResourceUnmarshaller = modelUnmarshaller[Disj[RevisionedCragResource]]

  implicit val cragUnmarshaller = modelUnmarshaller[Disj[Crag]]
  implicit val revisionedCragUnMarshaller = modelUnmarshaller[Disj[Revisioned[Crag]]]

  private def modelUnmarshaller[T : RootJsonReader]: Unmarshaller[T] =
    Unmarshaller.delegate[String, T](`application/json`, `application/hal+json`) { string =>
      string.asJson.convertTo[T]
    }

}
