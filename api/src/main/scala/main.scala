package org.freeclimbers.api

import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.io.IO

import spray.can.Http
import spray.routing.HttpService

import org.freeclimbers.core.{ActorSystemModule, ActorUsersModule, CoreActorSystemModule,
                              ProductionServices}

object Api {
  def main(args: Array[String]) = {

    val service = ProductionService.serviceActor
    implicit val system: ActorSystem = ProductionService.actorSystem
    IO(Http) ! Http.Bind(service, "localhost", port = 8080)

  }
}

trait HttpServiceActorModule {
  this: ActorSystemModule with AllRoutes =>

  val serviceActor = actorSystem.actorOf(
    Props(new ServiceActor()), "api-service")

  class ServiceActor extends Actor with HttpService {
    def actorRefFactory = actorSystem
    def receive = runRoute(routes)
  }
}

object ProductionService extends HttpServiceActorModule
                            with AllRoutes
                            with ProductionServices {

}

trait ServiceActorModule {
  this: ActorSystemModule with UserRoutes =>

  val serviceActor: ActorRef
}
