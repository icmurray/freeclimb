package freeclimb.restApi

import javax.sql.DataSource

import spray.json._
import spray.routing._
import spray.http._
import spray.httpx.marshalling._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

trait Routes extends HttpService {

  private val modelMarshaller = new BasicModelMarshallers(true)
  protected val api: CrudApi
  implicit protected val source: DataSource
  implicit protected val runner: ActionRunner

  lazy val routes = {
    import modelMarshaller._
    path("crags" / slug / "climbs" / slug) { (cragName, climbName) =>
      get {
        runRead { api.getClimb(cragName, climbName) }.fold(
          f       => complete(handleActionFailure(f)),
          success => complete(success)
        )
      }
    } ~
    path("crags" / slug) { cragName =>
      get {
        runRead { api.getCrag(cragName) }.fold(
          f       => complete(handleActionFailure(f)),
          success => complete(success)
        )
      }
    }
  }

  private def handleActionFailure(f: ActionFailure) = f match {
    case ValidationError() => HttpResponse(StatusCodes.BadRequest)
    case EditConflict()    => HttpResponse(StatusCodes.Conflict)
    case NotFound()        => HttpResponse(StatusCodes.NotFound)
    case NotImplemented()  => HttpResponse(StatusCodes.NotImplemented)
  }

  private lazy val slug = "[a-zA-Z0-9_-]+".r
}
