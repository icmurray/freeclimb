package freeclimb.restApi

import akka.actor.Actor

import spray.json._
import spray.routing._
import spray.http._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._

import freeclimb.api._
import freeclimb.models._

class ServiceActor extends Actor with Routes {
  def actorRefFactory = context
  def receive = runRoute(routes)
}

