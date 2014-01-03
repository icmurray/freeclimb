package org.freeclimbers.api

import scala.concurrent.Future
import scala.language.higherKinds

import org.freeclimbers.core.{UsersModule, RoutesDatabaseModule}

/**
 * Convenience trait that brings togeher all the route traits.
 */
trait AllRoutes[M[+_]] extends UserRoutes[M]
                          with ClimbRoutes[M]
                          with CragRoutes[M] {
  this: UsersModule[M] with HigherKindedUtils[M]
                       with RoutesDatabaseModule[M] =>

  lazy val routes = userRoutes ~ climbRoutes ~ cragRoutes

}

/**
 * Production-ready routes are all Future-based.
 */
trait ProductionRoutes extends AllRoutes[Future]
                          with FutureUtils {
  this: UsersModule[Future] with RoutesDatabaseModule[Future] =>
}
