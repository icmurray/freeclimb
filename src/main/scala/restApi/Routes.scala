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
  protected def runner: ActionRunner

  lazy val routes = {
    import modelMarshaller._
    path("crags" / slug / "climbs" / slug) { (cragName, climbName) =>
      get {
        runner.run { api.getClimbOption(cragName, climbName) }.fold(
          f       => complete(handleActionFailure(f)),
          success => success map {complete(_) } getOrElse complete(HttpResponse(StatusCodes.NotFound))
        )
      }
    } ~
    path("crags" / slug) { cragName =>
      get {
        runner.run { api.getCragOption(cragName) }.fold(
          f       => complete(handleActionFailure(f)),
          success => success map {complete(_)} getOrElse complete(HttpResponse(StatusCodes.NotFound))
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
