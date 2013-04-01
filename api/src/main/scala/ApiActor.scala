package org.freeclimbers.api

import akka.actor.Actor

import spray.routing._

import org.freeclimbers.api.routes.Routes

class ApiActor extends Actor
               with Routes {

  def actorRefFactory = context.system
  def receive = runRoute(routes)

}
