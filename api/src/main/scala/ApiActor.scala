package org.freeclimbers.api

import akka.actor.Actor

import spray.routing._

import org.freeclimbers.core.controllers.DefaultControllers

import org.freeclimbers.api.routes.Routes

class ApiActor extends Actor
               with Routes
               with DefaultControllers {

  def actorRefFactory = context.system
  def receive = runRoute(routes)

}
