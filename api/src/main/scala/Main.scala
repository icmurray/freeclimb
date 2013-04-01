package org.freeclimbers.api

import akka.actor.Props

import spray.can.server.SprayCanHttpServerApp

object Main extends App with SprayCanHttpServerApp {
  val handler = system.actorOf(Props[ApiActor])
  newHttpServer(handler) ! Bind(interface = "localhost", port = 8080)
}
