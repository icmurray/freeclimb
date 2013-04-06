package org.freeclimbers.api

import scala.concurrent.forkjoin.ForkJoinPool

import akka.actor.Actor

import spray.routing._

import org.freeclimbers.core.dal.DefaultDataAccessLayer

import org.freeclimbers.api.routes.Routes
import org.freeclimbers.api.controllers.DefaultControllers

class ApiActor extends Actor
               with DefaultDataAccessLayer
               with DefaultControllers
               with Routes {

  def actorRefFactory = context.system
  def receive = runRoute(routes)
  def controllerES = new ForkJoinPool()

}
