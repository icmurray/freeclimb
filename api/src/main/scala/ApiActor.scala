package org.freeclimbers.api

import akka.actor.Actor

import spray.routing._

import org.freeclimbers.core.dal.DefaultDataAccessLayer

import org.freeclimbers.api.routes.Routes

class ApiActor extends Actor
               with Routes
               with DefaultDataAccessLayer {

  def actorRefFactory = context.system
  def receive = runRoute(routes)

}
