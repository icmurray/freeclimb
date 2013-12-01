package org.freeclimbers.api

import scala.concurrent.Future
import scala.language.higherKinds

import spray.httpx.marshalling.{MetaMarshallers, Marshaller,
                                MetaToResponseMarshallers, ToResponseMarshaller}

import org.freeclimbers.core.UsersModule

trait AllRoutes[M[+_]] extends UserRoutes[M] {
  this: UsersModule[M] =>

  lazy val routes = userRoutes

}

trait ProductionRoutes extends AllRoutes[Future] {
  this: UsersModule[Future] =>

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def MarshallerM[T](implicit m: Marshaller[T]) = {
    MetaMarshallers.futureMarshaller
  }

  implicit def ToResponseMarshallerM[T](implicit m: ToResponseMarshaller[T]) = {

    /**
     * For some reason, the global ExecutionContext isn't being picked up,
     * so re-import it here ... until I figure out the problen.
     */
    import scala.concurrent.ExecutionContext.Implicits.global
    MetaToResponseMarshallers.futureMarshaller
  }

}
