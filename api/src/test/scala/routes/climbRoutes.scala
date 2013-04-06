package org.freeclimbers.api.routes

import spray.http.StatusCodes._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen

import org.scalamock.scalatest.MockFactory

import org.freeclimbers.core.controllers.ClimbController
import org.freeclimbers.core.models.Climb
import org.freeclimbers.api.{PaginationRequest, PagedResponse}

class ClimbRoutesApiSpec extends FeatureSpec
                         with GivenWhenThen
                         with MockFactory
                         with ScalatestRouteTest {

  feature("An API user can page through all available climbs") {

    info("As an anonymous user")
    info("I want to list all available climbs")

    scenario("all the climbs fit on one page") {
      Given("10 climbs in the database")
      val controller = mock[ClimbController]
      (controller.getPage _).expects(10, 0).returning(List(),0)
      val routes = new ClimbRoutes {
        def actorRefFactory = system
        def climbController = controller
      }
      import routes._

      Get("/climbs?limit=10") ~> routes.climbRoutes ~> check {
        When("/climbs?limit=10 is accessed")
        Then("the response should be successful")
        assert(status === OK)
        
        And("it should contain all 10 climbs")
        val result = entityAs[PagedResponse[Climb]]
        assert(result.count === 10)
        assert(result.payload.toSet === List().toSet)
      }

    }

    scenario("climbs are split across 2 pages") {
      Given("10 climbs in the database")
      When("/climbs?limit=5 is accessed")
      Then("the response should be successful")
      And("it should contain the first 5 climbs")
      When("/climbs?limit=5&offset=5 is accessed")
      Then("the response should be successful")
      And("it should contain the remaining climbs")
      pending
    }

  }

}
