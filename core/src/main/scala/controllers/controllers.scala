package org.freeclimbers.core.controllers

trait Controllers extends ClimbControllerComponent

trait DefaultControllers extends Controllers {

  lazy val climbController = new ClimbController {
    def getPage(limit: Long, offset: Long) = { sys.error("Not implemented") }
  }

}
