package org.freeclimbers.api.routes

import scala.concurrent.future

import spray.http.StatusCodes._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen

import org.scalamock.scalatest.MockFactory

import org.freeclimbers.core.models.Climb
import org.freeclimbers.api.{PaginationRequest, PagedResponse}
import org.freeclimbers.api.controllers.{ClimbController, ClimbControllerComponent}

class ClimbRoutesApiSpec extends FeatureSpec
                         with GivenWhenThen
                         with MockFactory
                         with ScalatestRouteTest {

  feature("An API user can page through all available climbs") {

    info("As an anonymous user")
    info("I want to list all available climbs")

    scenario("all the climbs fit on one page") {
      Given("4 climbs in the database")
      val controller = mock[ClimbController]
      (controller.getPage _).expects(PaginationRequest(4,0))
                            .returning(future {
                              PagedResponse[Climb](
                                count = initClimbs.length,
                                payload = initClimbs)
                            })
      val routes = new ClimbRoutes with ClimbControllerComponent {
        override def actorRefFactory = system
        override def climbController = controller
      }
      import routes._

      Get("/climbs?limit=4") ~> routes.climbRoutes ~> check {
        When("/climbs?limit=4 is accessed")
        Then("the response should be successful")
        assert(status === OK)
        
        And("it should contain all 4 climbs")
        val result = entityAs[PagedResponse[Climb]]
        assert(result.count === 4)
        assert(result.payload.toSet === initClimbs.toSet)
      }

    }

    scenario("climbs are split across 2 pages") {
      Given("4 climbs in the database")
      val controller = mock[ClimbController]
      (controller.getPage _).expects(PaginationRequest(2,0))
                            .returning(future { 
                              PagedResponse[Climb](
                                count = initClimbs.length,
                                payload = initClimbs.slice(0,2))
                            })
      (controller.getPage _).expects(PaginationRequest(2,2))
                            .returning(future {
                              PagedResponse[Climb](
                                count = initClimbs.length,
                                payload = initClimbs.slice(2,4))
                            })
      val routes = new ClimbRoutes with ClimbControllerComponent {
        override def actorRefFactory = system
        override def climbController = controller
      }
      import routes._

      Get("/climbs?limit=2") ~> routes.climbRoutes ~> check {
        When("/climbs?limit=2 is accessed")
        Then("the response should be successful")
        assert(status === OK)

        And("it should contain the first 2 climbs")
        val result = entityAs[PagedResponse[Climb]]
        assert(result.count === 4)
        assert(result.payload.toSet === initClimbs.slice(0,2).toSet)
      }

      Get("/climbs?limit=2&offset=2") ~> routes.climbRoutes ~> check {
        When("/climbs?limit=2&offset=2 is accessed")
        Then("the response should be successful")
        assert(status === OK)

        And("it should contain the remaining climbs")
        val result = entityAs[PagedResponse[Climb]]
        assert(result.count === 4)
        assert(result.payload.toSet === initClimbs.slice(2,4).toSet)
      }
    }

  }

  private val initClimbs = ('a' to 'd').map(s => Climb(s"Climb ${s}"))

}
