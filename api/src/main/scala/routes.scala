package org.freeclimbers.api

import scala.concurrent.Future

import org.freeclimbers.core.UsersModule

trait AllRoutes extends UserRoutes {
  this: UsersModule[Future] =>

  lazy val routes = userRoutes

}
