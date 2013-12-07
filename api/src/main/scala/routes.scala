package org.freeclimbers.api

import scala.concurrent.Future
import scala.language.higherKinds

import org.freeclimbers.core.UsersModule

/**
 * Convenience trait that brings togeher all the route traits.
 */
trait AllRoutes[M[+_]] extends UserRoutes[M] {
  this: UsersModule[M] with HigherKindedMarshalling[M] =>

  lazy val routes = userRoutes

}

/**
 * Production-ready routes are all Future-based.
 */
trait ProductionRoutes extends AllRoutes[Future]
                          with FutureMarshalling {
  this: UsersModule[Future] =>

  def readM[T](t: Future[T]) = t

}
