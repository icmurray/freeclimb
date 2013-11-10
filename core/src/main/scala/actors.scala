package org.freeclimbers.core

import akka.actor._

trait ActorSystemModule {
  val actorSystem: ActorSystem
  implicit val ec = actorSystem.dispatcher
}
