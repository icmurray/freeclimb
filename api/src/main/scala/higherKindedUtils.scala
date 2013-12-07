package org.freeclimbers.api

import scala.concurrent.{Future, future}
import scala.language.higherKinds

import scalaz._
import Scalaz._

import spray.httpx.marshalling.{MetaMarshallers, Marshaller,
                                MetaToResponseMarshallers, ToResponseMarshaller}

trait HigherKindedUtils[M[+_]] extends HigherKindedMarshalling[M] {
  def readM[T](t: M[T]): Future[T]
}

/**
 * Provides the mixed-in trait/class with a ToResponseMarshaller[M[T]] for
 * marshalling their polymorphic monad type.
 */
trait HigherKindedMarshalling[M[+_]] {
  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T])
                 : ToResponseMarshaller[M[T]]
}

trait FutureUtils extends HigherKindedUtils[Future] {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = {
    MetaToResponseMarshallers.futureMarshaller
  }

  override def readM[T](t: Future[T]) = t
}

trait IdUtils extends HigherKindedUtils[Id] {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = m
  override def readM[T](t: T) = future { t }
}

