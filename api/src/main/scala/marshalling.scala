package org.freeclimbers.api

import spray.json._
import spray.http.HttpBody
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller
import spray.httpx.SprayJsonSupport
import spray.http.MediaTypes._

import org.freeclimbers.core.models._

trait Marshalling {

  val `application/vnd.org.freeclimber-v1` =
          register(CustomMediaType("application/vnd.org.freeclimber-v1"))

  implicit def MyMarshaller[T](implicit writer: RootJsonWriter[T], printer: JsonPrinter = PrettyPrinter) =
    Marshaller.delegate[T,String](
        `application/vnd.org.freeclimber-v1`) { value =>

      val json = writer.write(value)
      printer(json)

    }

  implicit def MyUnMarshaller[T:RootJsonReader] = {
    Unmarshaller[T](`application/vnd.org.freeclimber-v1`) {
      case x: HttpBody =>
        val json = JsonParser(x.asString)
        jsonReader[T].read(json)
    }
  }
    

  //implicit val ClimbMarshaller = Marshaller.of[Climb](
  //  `application/vnd.org.freeclimber-v1`) { (value, contentType, ctx) =>
  //    val Climb(name) = value
  //    val string = s"name:${name}"
  //    ctx.marshalTo(HttpBody(contentType, string))
  //  }

  //implicit def PageMarshaller[T:Marshaller] = Marshaller.of[Page[T]](
  //  `application/vnd.org.freeclimber-v1`) { (value, contentType, ctx) =>
  //    val Page(count, payload, links) = value
  //    val string = s"count:${}\npayload:${payload}\nlinks:${links}"
  //    ctx.marshalTo(HttpBody(contentType, string))
  //  }

}

