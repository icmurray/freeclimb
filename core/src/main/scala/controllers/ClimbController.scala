package org.freeclimbers.core.controllers

import org.freeclimbers.core.models.Climb

trait ClimbControllerComponent {

  def climbController: ClimbController

}

trait ClimbController {
  
  def getPage(limit: Long, offset: Long): (Seq[Climb], Long)

}
