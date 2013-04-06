package org.freeclimbers.api.routes

import org.freeclimbers.api.controllers.Controllers

trait Routes extends ClimbRoutes { this: Controllers =>

  val routes = climbRoutes

}
