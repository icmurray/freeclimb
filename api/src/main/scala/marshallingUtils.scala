package org.freeclimbers.api

import scalaz._
import Scalaz._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport

import spray.http.{StatusCode, StatusCodes, ContentTypes}
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}

import org.freeclimbers.core.{Validated, DomainError}

trait MarshallingUtils extends DomainErrorMarshalling

trait DomainErrorMarshalling {

  private val domainErrorJson = new RootJsonWriter[DomainError] {
    def write(e: DomainError) = JsObject(
      "errors" -> JsArray(e.map(msg => JsString(msg)))
    )
  }

  implicit def validationResponseMarshaller[T](implicit mT: ToResponseMarshaller[T])
                 : ToResponseMarshaller[Validated[T]] = {

    ToResponseMarshaller.of[Validated[T]](ContentTypes.`application/json`) {
        (value, contentType, ctx) =>
      value match {
        case Success(t) => mT(t, ctx)
        case Failure(e) =>

          // Need to be explicit about the DomainError marshaller, as a
          // DomainError is just an alias for List[String].
          val domainErrorResponseMarshaller = ToResponseMarshaller.fromMarshaller(
            status=StatusCodes.BadRequest)(
            SprayJsonSupport.sprayJsonMarshaller[DomainError](domainErrorJson))
          domainErrorResponseMarshaller(e, ctx)
      }
    }
  }
}
