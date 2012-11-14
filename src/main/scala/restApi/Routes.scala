package freeclimb.restApi

import javax.sql.DataSource

import scalaz._
import Scalaz._

import spray.json._
import spray.routing._
import spray.http._
import spray.http.HttpHeaders._
import spray.httpx.marshalling._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._
import freeclimb.validation._

trait Routes extends HttpService {
  

  private val modelMarshaller = new BasicModelMarshallers(true)
  import modelMarshaller._

  protected val api: CrudApi
  protected def runner: ActionRunner

  private def run[A,I<:IsolationLevel](action: ApiAction[A,I])
                                            (onSuccess: A => Route)
                                            (implicit m: Manifest[I]) = {
    runner.run(action).fold(
      failure => complete(handleActionFailure(failure)),
      success => onSuccess(success)
    )
  }

  lazy val routes = {
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
        run(api.getCragOption(cragName)) { success =>
          success map { crag =>

            respondWithHeader(ETag(crag.revision.toString)) {
              headerValue(ifNoneMatch) { revision =>
                if (revision  == crag.revision.toString) {
                  complete(HttpResponse(StatusCodes.NotModified))
                } else { reject }
              } ~ complete(crag) 
            }

          } getOrElse complete(HttpResponse(StatusCodes.NotFound))
        }
      } ~
      //put {
      //  entity(as[Disj[RevisionedCragResource]]) { resourceToCragRevision(_, cragName).fold(
      //    errors   => complete(StatusCodes.BadRequest, errors),
      //    revision => runner.run { api.updateCrag(revision) }.fold(
      //      failure     => complete(handleActionFailure(failure)),
      //      newRevision => complete(newRevision)
      //    )
      //  )}
      //} ~
      put {
        entity(as[Disj[CragResource]]) { resourceToCrag(_, cragName).fold(
          errors => complete(StatusCodes.BadRequest, errors),
          crag   => runner.run { api.createCrag(crag) }.fold(
            failure  => complete(handleActionFailure(failure)),
            revision => complete(StatusCodes.Created, revision)
          )
        )}
      }
    }
  }

  private def ifNoneMatch(header: HttpHeader): Option[String] = header.lowercaseName match {
    case "if-none-match" => Some(header.value)
    case _               => None
  }

  private def resourceToCragRevision(resourceD: Disj[RevisionedCragResource], cragName: String) = {
    resourceD >>= { resource: RevisionedCragResource =>
      Crag(cragName, resource.title) map { crag: Crag =>
        Revisioned[Crag](resource.revision.longValue, crag)
      }
    }
  }

  private def resourceToCrag(resourceDisjunction: Disj[CragResource], cragName: String) = {
    resourceDisjunction >>= { resource: CragResource =>
      Crag(cragName, resource.title)
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
  
private case class ETag(override val value: String) extends RawHeader("ETag", value)

