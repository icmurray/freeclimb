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

import org.freeclimbers.core.{Crag, CragId, CragsModule}

class CragRoutesSpec extends FlatSpec with ShouldMatchers
                                      with ScalatestRouteTest
                                      with MockFactory {


  "POST to /crags" should "create new crags" in {
    withCragsEndpoint { module =>

      val name = "Stanage"
      val description = "Pretty good"
      val crag = newCrag(name, description)

      val json = JsonEntity(s"""
        {
          "name": "${name}",
          "description": "${description}"
        }
      """)

      (module.crags.create _)
        .expects(name, description)
        .returning(crag.id.success)

      Post("/crags", json) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.Created)
        val json = responseAs[JsObject]
        json.fields("id") should equal(JsString(crag.id.uuid.toString))
      }
    }
  }

  "POST to /crags" should "response with BadRequest if crag is invalid" in {
    withCragsEndpoint { module =>

      val cragId = CragId.createRandom()

      val json = JsonEntity(s"""
        {
          "name": "",
          "description": "Not blank"
        }
      """)

      (module.crags.create _)
        .expects("", "Not blank")
        .returning(List("name cannot be blank").failure)

      Post("/crags", json) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
      }
    }
  }

  "GET /crags/<uuid>" should "respond with an existing crag" in {
    withCragsEndpoint { module =>

      val id = CragId.createRandom()
      val crag = Crag(id, "Stanage", "It's pretty nice here.")

      (module.crags.withId _)
        .expects(id)
        .returning(Some(crag))

      Get("/crags/" + id.uuid.toString) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.OK)
        val json = responseAs[JsObject]
        json.fields("id") should equal(JsString(id.uuid.toString))
        json.fields("name") should equal(JsString(crag.name))
      }
    }
  }

  "GET /crags/<uuid>" should "respond with 404 if not found" in {
    withCragsEndpoint { module =>

      val id = CragId.createRandom()

      (module.crags.withId _)
        .expects(id)
        .returning(None)

      Get("/crags/" + id.uuid.toString) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.NotFound)
      }
    }
  }

  private def newCrag(name: String, description: String = "") = {
    Crag(CragId.createRandom(), name, description)
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withCragsEndpoint(f: CragRoutes[Id] with CragsModule[Id] => Unit) = {

    val module = new CragRoutes[Id] with CragsModule[Id] with IdUtils with HttpService {
      def actorRefFactory = system
      override def cragRoutes = sealRoute(super.cragRoutes)
      override val crags = mock[CragService]
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