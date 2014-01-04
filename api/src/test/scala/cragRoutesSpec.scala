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

import org.freeclimbers.core.{Crag, CragId, RoutesDatabaseModule, ClimbId}
import org.freeclimbers.core.{DomainError, Validated}

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

      (module.routesDB.createCrag _)
        .expects(name, description)
        .returning(CResult(crag.id.right))

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

      (module.routesDB.createCrag _)
        .expects("", "Not blank")
        .returning(CResult(List("name cannot be blank").left))

      Post("/crags", json) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.BadRequest)
      }
    }
  }

  "GET /crags/<uuid>" should "respond with an existing crag" in {
    withCragsEndpoint { module =>

      val id = CragId.createRandom()
      val climbIds = Set(ClimbId.createRandom(), ClimbId.createRandom())
      val crag = Crag(id, "Stanage", "It's pretty nice here.", climbIds)

      (module.routesDB.cragById _)
        .expects(id)
        .returning(Some(crag))

      Get("/crags/" + id.uuid.toString) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.OK)
        val json = responseAs[JsObject]
        json.fields("id") should equal(JsString(id.uuid.toString))
        json.fields("name") should equal(JsString(crag.name))
        json.fields("description") should equal(JsString(crag.description))

        val expectedLink = CragClimbsListingLink.ofCrag(id)
        json.fields("climbs").asInstanceOf[JsObject]
            .fields("href") should equal (JsString(expectedLink.href))
      }
    }
  }

  "GET /crags/<uuid>" should "respond with 404 if not found" in {
    withCragsEndpoint { module =>

      val id = CragId.createRandom()

      (module.routesDB.cragById _)
        .expects(id)
        .returning(None)

      Get("/crags/" + id.uuid.toString) ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.NotFound)
      }
    }
  }

  "GET /crags" should "list all crags" in {
    withCragsEndpoint { module =>

      val someCrags = List(
        Crag(CragId.createRandom(), "Crag 1", "", Set(ClimbId.createRandom())),
        Crag(CragId.createRandom(), "Crag 2", "", Set(ClimbId.createRandom())),
        Crag(CragId.createRandom(), "Crag 3", "", Set(ClimbId.createRandom())))

      (module.routesDB.crags _)
        .expects()
        .returning(someCrags)

      Get("/crags") ~> module.cragRoutes ~> check {
        status should equal (StatusCodes.OK)
        val json = responseAs[JsObject]
        json.fields("total") should equal(JsNumber(3))
      }
    }
  }

  private def newCrag(name: String, description: String = "") = {
    Crag(CragId.createRandom(), name, description, Set())
  }

  private def JsonEntity(s: String) = HttpEntity(`application/json`, s)

  private def withCragsEndpoint(f: CragRoutes[Id] with RoutesDatabaseModule[Id] => Unit) = {

    val module = new CragRoutes[Id] with RoutesDatabaseModule[Id] with IdUtils with HttpService {
      def actorRefFactory = system
      override def cragRoutes = sealRoute(super.cragRoutes)
      override val routesDB = mock[RoutesDBService]
      implicit def M = id
    }

    f(module)
  }

  private[this] def CResult[T](t: Validated[T]) = EitherT[Id, DomainError, T](t)

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] = {
    Unmarshaller.delegate[String, JsObject](MediaTypes.`application/json`) { string =>
      string.asJson.asJsObject
    }
  }
}
