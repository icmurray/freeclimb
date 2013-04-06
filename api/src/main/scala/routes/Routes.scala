package org.freeclimbers.api.routes

import org.freeclimbers.core.dal.DataAccessLayer

trait Routes extends ClimbRoutes { this: DataAccessLayer =>

  val routes = climbRoutes

}
