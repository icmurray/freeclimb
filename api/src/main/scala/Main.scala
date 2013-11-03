package org.freeclimbers.api

import akka.actor.ActorSystem

import spray.routing.SimpleRoutingApp

object Main extends SimpleRoutingApp
               with Routes {

  implicit val actorSystem = ActorSystem("rest-api")

  startServer(interface="localhost", port=8080)(routes)

}
