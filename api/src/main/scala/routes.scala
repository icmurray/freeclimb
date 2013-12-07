package org.freeclimbers.api

import scala.concurrent.Future
import scala.language.higherKinds

import org.freeclimbers.core.UsersModule

/**
 * Convenience trait that brings togeher all the route traits.
 */
trait AllRoutes[M[+_]] extends UserRoutes[M] {
  this: UsersModule[M] with HigherKindedUtils[M] =>

  lazy val routes = userRoutes

}

/**
 * Production-ready routes are all Future-based.
 */
trait ProductionRoutes extends AllRoutes[Future]
                          with FutureUtils {
  this: UsersModule[Future] =>
}
