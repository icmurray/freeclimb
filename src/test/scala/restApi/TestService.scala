package freeclimb.restApi

import org.scalatest.{FunSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import spray.httpx.unmarshalling._
import spray.http.MediaTypes._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes._

import freeclimb.models._

import freeclimb.test.common._

class ServiceTest extends FunSpec
                     with Routes
                     with BeforeAndAfter
                     with ShouldMatchers
                     with MimeTypes
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

        it("Should contain the revision in the representation") {
          Get("/crags/burbage") ~> routes ~> check {
            val json = entityAs[JsObject]
            val revision = json.fields.get("revision")
            revision should not equal (None)
            revision.get.asInstanceOf[JsNumber].value should equal (1L)
          }
        }

        it("Should describe the requested crag as JSON") {
          Get("/crags/burbage") ~> routes ~> check {
            val json = entityAs[JsObject]

            val name = json.fields.get("name")
            name should not equal (None)
            name.get.asInstanceOf[JsString].value should equal(burbage.name)

            val title = json.fields.get("title")
            title should not equal (None)
            title.get.asInstanceOf[JsString].value should equal(burbage.title)
          }
        }

        it("Should 304 (Not Modified) if given a matching ETag in If-None-Match") {
          Get("/crags/burbage") ~> addHeader("If-None-Match", "1") ~> routes ~> check {
            status should equal (NotModified)

            val etag = header("ETag")
            etag should not equal (None)
            etag.get.value.toLong should equal (1L)
          }
        }

        it("Should 200 if ETag does not match") {
          Get("/crags/burbage") ~> addHeader("If-None-Match", "0") ~> routes ~> check {
            status should equal (OK)

            val etag = header("ETag")
            etag should not equal (None)
            etag.get.value.toLong should equal (1L)
          }
        }
      }

      describe("PUT-ing a new resource") {
        it("Should create a new Crag if one doesn't exist already") (pending)
        it("Should require either the If-None-Match or If-Match header") (pending)
        it("Should reject the request if the Crag already exists") (pending)
        it("Should reject the request if the Crag is invalid") (pending)
        it("Should reject the request if the body isn't valid json") (pending)
        it("Should reject the request if the body is missing a required field") (pending)
      }
    }
  }

  private def burbage = Crag.makeUnsafe("burbage", "Burbage")

  implicit private val JsonUnmarshaller: Unmarshaller[JsObject] =
    Unmarshaller.delegate[String, JsObject](`application/json`, `application/hal+json`) { string =>
      string.asJson.asJsObject
    }
}
