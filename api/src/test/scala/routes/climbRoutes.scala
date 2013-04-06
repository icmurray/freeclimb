package org.freeclimbers.api.routes

import spray.http.StatusCodes._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen

import org.scalamock.scalatest.MockFactory

import org.freeclimbers.core.dal.ClimbRepository
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
      Given("4 climbs in the database")
      val repo = mock[ClimbRepository]
      (repo.getPage _).expects(4, 0).returning(initClimbs,4)
      val routes = new ClimbRoutes {
        override def actorRefFactory = system
        override def climbRepo = repo
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
      val Repo = mock[ClimbRepository]
      (Repo.getPage _).expects(2, 0).returning(initClimbs.slice(0,2),4)
      (Repo.getPage _).expects(2, 2).returning(initClimbs.slice(2,4),4)
      val routes = new ClimbRoutes {
        override def actorRefFactory = system
        override def climbRepo = Repo
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
