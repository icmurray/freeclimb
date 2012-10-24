package freeclimb.restApi

import spray.json._
import spray.routing._
import spray.http._
import spray.httpx.marshalling._

import freeclimb.models._

trait Routes extends HttpService {

  private val modelMarshaller = new BasicModelMarshallers(true)

  lazy val routes = {
    import modelMarshaller._
    path("crags" / slug / "climbs" / slug) { (cragName, climbName) =>
      get {
        complete {
          val crag = Crag.makeUnsafe(cragName, cragName)
          val climb = Climb.makeUnsafe(climbName, climbName, "desc", crag, UkTrad(Grade.UkAdjective.E2, Grade.UkTechnical.T5c))
          Revisioned[Climb](1L, climb)
        }
      }
    } ~
    path("crags" / slug) { cragName =>
      get {
        complete {
          Revisioned[Crag](1L, Crag.makeUnsafe(cragName, cragName))
        }
      }
    }
  }


  private lazy val slug = "[a-zA-Z0-9_-]+".r
}
