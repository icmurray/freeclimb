package org.freeclimbers.api

import scala.concurrent.{Future, future}

import org.scalamock.scalatest.MockFactory

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.httpx.unmarshalling.Unmarshaller
import ContentTypes._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import scalaz._
import Scalaz._

import org.freeclimbers.core.{Climb, ClimbId, ClimbsModule, CragId}

class ClimbRoutesSpec extends FlatSpec with ShouldMatchers
                                       with ScalatestRouteTest
                                       with MockFactory {


  "POST to /climbs" should "create new climbs" in {
    withClimbsEndpoint { module =>

      val name = "Harvest"
      val description = "Pretty hard"
      val cragId = CragId.createRandom()

      val json = JsonEntity(s"""
        {
          "name": "${name}",
          "description": "${description}",
          "crag_id": "${cragId.uuid.toString}"
        }
      """)

      val climb = newClimb(name)

      (module.climbs.create _)
        .expects(name, description, cragId)
        .returning(climb.id.success)

      Post("/climbs", json) ~> module.climbRoutes ~> check {
        status should equal (StatusCodes.Created)
        val json = responseAs[JsObject]
        json.fields("id") should equal(JsString(climb.id.uuid.toString))
      }
    }
  }

  "POST to /climbs" should "response with BadRequest if climb is invalid" in {
    withClimbsEndpoint { module =>

      val cragId = CragId.createRandom()

      val json = JsonEntity(s"""
        {
          "name": "",
          "description": "Not blank",
          "crag_id": "${cragId.uuid.toString}"
        }
      """)

      (module.climbs.create _)
        .expects("", "Not blank", cragId)
        .returning(List("name cannot be blank").failure)

      Post("/climbs", json) ~> module.climbRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
      }
    }
  }

  "GET /climbs/<uuid>" should "respond with an existing climb" in {
    withClimbsEndpoint { module =>

      val id = ClimbId.createRandom()
      val cragId = CragId.createRandom()
      val climb = Climb(id, cragId, "Harvest", "")

      (module.climbs.withId _)
        .expects(id)
        .returning(Some(climb))

      Get("/climbs/" + id.uuid.toString) ~> module.climbRoutes ~> check {
        status should equal (StatusCodes.OK)
        val json = responseAs[JsObject]
        json.fields("id") should equal(JsString(id.uuid.toString))
        json.fields("name") should equal(JsString(climb.name))
      }
    }
  }

  "GET /climbs/<uuid>" should "respond with 404 if not found" in {
    withClimbsEndpoint { module =>

      val id = ClimbId.createRandom()
      val cragId = CragId.createRandom()
      val climb = Climb(id, cragId, "Harvest", "")

      (module.climbs.withId _)
        .expects(id)
        .returning(None)

      (module.climbs.resolvesTo _)
        .expects(id)
        .returning(None)

      Get("/climbs/" + id.uuid.toString) ~> module.climbRoutes ~> check {
        status should equal (StatusCodes.NotFound)
      }
    }
  }

  "GET /climbs/<uuid>" should "redirect when a climb has been de-duplicated" in {
    withClimbsEndpoint { module =>

      val removedId = ClimbId.createRandom()
      val id = ClimbId.createRandom()
      val cragId = CragId.createRandom()
      val climb = Climb(id, cragId, "Harvest", "")

      (module.climbs.withId _)
        .expects(removedId)
        .returning(None)

      (module.climbs.resolvesTo _)
        .expects(removedId)
        .returning(Some(climb))

      Get("/climbs/" + removedId.uuid.toString) ~> module.climbRoutes ~> check {
        status should equal (StatusCodes.MovedPermanently)
      }
    }
  }

  private def newClimb(name: String, description: String = "") = {
    Climb(ClimbId.createRandom(), CragId.createRandom(), name, description)
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withClimbsEndpoint(f: ClimbRoutes[Id] with ClimbsModule[Id] => Unit) = {

    val module = new ClimbRoutes[Id] with ClimbsModule[Id] with IdUtils with HttpService {
      def actorRefFactory = system
      override def climbRoutes = sealRoute(super.climbRoutes)
      override val climbs = mock[ClimbService]
      implicit def M = id
    }

    f(module)
  }

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] = {
    Unmarshaller.delegate[String, JsObject](MediaTypes.`application/json`) { string =>
      string.asJson.asJsObject
    }
  }
}
