package freeclimb.restApi

import org.scalatest.{FunSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._

import freeclimb.models._

import freeclimb.test.common._

class ServiceTest extends FunSpec
                     with Routes
                     with BeforeAndAfter
                     with ShouldMatchers
                     with ScalatestRouteTest {

  override val api = new CrudApiMock()
  override def runner = new ActionRunnerMock()

  def actorRefFactory = system

  before {
    api.reset()
  }

  describe("The Service") {
    describe("The crag resource") {

      describe("Should return 404 if crag does not exist") {
        Get("/crags/does-not-exist") ~> routes ~> check {
          status should equal (NotFound)
        }
      }

      describe("Should 200 if the crag exists") {

        runner.run { api.createCrag(burbage) }

        Get("/crags/burbage") ~> routes ~> check {
          status should equal (OK)
        }
      }

    }
  }

  private def burbage = Crag.makeUnsafe("burbage", "Burbage")

}
