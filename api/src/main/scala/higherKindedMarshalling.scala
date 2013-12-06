package org.freeclimbers.api

import scala.concurrent.Future
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.httpx.marshalling.{MetaMarshallers, Marshaller,
                                MetaToResponseMarshallers, ToResponseMarshaller}

/**
 * Provides the mixed-in trait/class with a ToResponseMarshaller[M[T]] for
 * marshalling their polymorphic monad type.
 */
trait HigherKindedMarshalling[M[+_]] {
  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T])
                 : ToResponseMarshaller[M[T]]
}

trait FutureMarshalling extends HigherKindedMarshalling[Future] {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = {
    MetaToResponseMarshallers.futureMarshaller
  }
}

trait IdMarshalling extends HigherKindedMarshalling[Id] {
  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = m
}

