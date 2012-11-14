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
    runner.run { api.createCrag(burbage) }
  }

  describe("The Service") {
    describe("/crags/<crag>") {
      describe("GET-ing a crag resource") {

        it("Should 404 if the crag does not exist") {
          Get("/crags/does-not-exist") ~> routes ~> check {
            status should equal (NotFound)
          }
        }

        it("Should 200 if the crag exists") {
          Get("/crags/burbage") ~> routes ~> check {
            status should equal (OK)
          }
        }

        it("Should contain the revision in the ETag header") {
          Get("/crags/burbage") ~> routes ~> check {
            val etag = header("ETag")
            etag should not equal (None)
            etag.get.value.toLong should equal (1L)
          }
        }

        it("Should contain the revision in the representation") (pending)
        it("Should describe the requested crag as JSON") (pending)

      }
    }
  }

  private def burbage = Crag.makeUnsafe("burbage", "Burbage")

}
