package org.freeclimbers.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

import spray.httpx.marshalling.{MetaMarshallers, Marshaller}

import org.freeclimbers.core.UsersModule

trait AllRoutes[M[+_]] extends UserRoutes[M] {
  this: UsersModule[M] =>

  lazy val routes = userRoutes

}

trait ProductionRoutes extends AllRoutes[Future] {
  this: UsersModule[Future] =>

  implicit def MarshallerM[T](implicit m: Marshaller[T]) = {
    MetaMarshallers.futureMarshaller
  }

}
