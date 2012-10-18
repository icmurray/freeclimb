package freeclimb.restApi

import akka.actor.Actor

import spray.routing._
import spray.http._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._

import freeclimb.api._
import freeclimb.models._

class ServiceActor extends Actor with ApiRoutes {
  def actorRefFactory = context
  def receive = runRoute(routes)
}

trait CragRoutes extends HttpService {
  val cragRoutes =
    path("crags" / "[a-zA-Z0-9_-]+".r / "climbs" / "[a-zA-Z0-9_-]+".r) { (cragName, climbName) =>
      get {
        complete {
          import RevisionedClimbResource._
          import ClimbResource._
          val crag = Crag.makeUnsafe(cragName, cragName)
          val climb = Climb.makeUnsafe(climbName, climbName, "desc", crag, UkTrad(Grade.UkAdjective.E2, Grade.UkTechnical.T5c))
          ClimbResource(climb).toJson
          //marshal(RevisionedClimbResource(Revisioned[Climb](1L, climb)))
        }
      }
    } ~
    path("crags" / "[a-zA-Z0-9_-]+".r) { name =>
      get {
        complete {
          import CragResource._
          val crag = Crag.makeUnsafe(name, name)
          marshal(Revisioned[Crag](1L, crag))
          marshal(RevisionedCragResource(Revisioned[Crag](1L, crag)))
        }
      }
    }
}

trait ApiRoutes extends CragRoutes{

  val routes = cragRoutes

}
