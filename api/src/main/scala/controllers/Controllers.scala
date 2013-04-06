package org.freeclimbers.api.controllers

import java.util.concurrent.ExecutorService

import scala.concurrent.ExecutionContext

import org.freeclimbers.core.dal.DataAccessLayer

trait Controllers extends ClimbControllerComponent

trait DefaultControllers extends Controllers { this: DataAccessLayer =>

  // The ExecutorService to execute the controller-layer's Future
  // computations on.
  def controllerES: ExecutorService
  private lazy val controllerEC = ExecutionContext.fromExecutorService(controllerES)

  override lazy val climbController = new DefaultClimbController(controllerEC, this.climbRepo)

}
