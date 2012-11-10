package freeclimb.restApi

import javax.sql.DataSource

import scalaz._
import Scalaz._

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
          failure => complete(handleActionFailure(failure)),
          success => success map {complete(_)} getOrElse complete(HttpResponse(StatusCodes.NotFound))
        )
      } ~
      put {
        entity(as[RichValidation[String, Revisioned[Crag]]]) { revision => revision.fold(
          errors   => complete(StatusCodes.BadRequest, errors),
          revision => runner.run { api.updateCrag(revision) }.fold(
            failure     => complete(handleActionFailure(failure)),
            newRevision => complete(newRevision)
          )
        )}
      } ~
      post {
        entity(as[RichValidation[String, Crag]]) { crag => crag.fold(
          errors => complete(StatusCodes.BadRequest, errors),
          crag   => runner.run { api.createCrag(crag) }.fold(
            failure  => complete(handleActionFailure(failure)),
            revision => complete(StatusCodes.Created, revision)
          )
        )}
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
