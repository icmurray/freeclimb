package org.freeclimbers.core

import scala.concurrent.Future

import scalaz.contrib.std.scalaFuture

trait ProductionServices extends ActorUsersModule
                            with EventsourcedClimbsModule
                            with EventsourcedRoutesDatabaseModule
                            with CoreActorSystemModule {
  implicit def M = scalaFuture.futureInstance
}
