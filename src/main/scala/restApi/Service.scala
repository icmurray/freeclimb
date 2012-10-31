package freeclimb.restApi

import javax.sql.DataSource

import akka.actor.Actor

import spray.json._
import spray.routing._
import spray.http._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._

import freeclimb.api._
import freeclimb.models._

class ServiceActor(override val source: DataSource) extends Actor with Routes {

  override protected val api = CrudApi
  override protected val runner = NotifyingActionRunner

  def actorRefFactory = context
  def receive = runRoute(routes)
}
