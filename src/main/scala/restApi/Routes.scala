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

    path("climbs" / slug / slug ) { (cragName, climbName) =>
      get {
        run(api.getClimbOption(cragName, climbName)) { success =>
          success map { climb =>
            respondWithHeader(ETag(climb.revision.toString)) {
              headerValue(ifNoneMatch) { revision =>
                if (revision  == climb.revision.toString) {
                  complete(HttpResponse(StatusCodes.NotModified))
                } else { reject }
              } ~ complete(climb) 
            }
          } getOrElse complete(HttpResponse(StatusCodes.NotFound))
        }
      }
    } ~
    path("crags") {
      get {
        run(api.listCrags()) { crags =>
          complete(crags)
        }
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
      put {
        entity(as[Disj[CragResource]]) { resourceToCrag(_, cragName).fold(
          errors => complete(StatusCodes.BadRequest, errors),

          crag   => {
            headerValue(resourceCreation) { _ =>
              runner.run { api.createCrag(crag) }.fold(
                  failure  => complete(handleActionFailure(failure)),
                  revision => complete(StatusCodes.Created, revision)
              )
            } ~
            headerValue(resourceUpdate) { revision =>
              val currentRevision = Revisioned[Crag](revision, crag)
              runner.run { api.updateCrag(currentRevision) }.fold(
                failure     => complete(handleActionFailure(failure)),
                newRevision => complete(newRevision)
              )
            } ~ complete(StatusCodes.PreconditionRequired, """
                  Make request with 'If-None-Match: *' header to create a new Crag
                  Make request with 'If-Match: <rev>' header to update an existing Crag""")
          }

        )}
      }
    }
  }

  private def resourceCreation(header: HttpHeader): Option[String] = header.lowercaseName match {
    case "if-none-match" if header.value == "*" => Some(header.value)
    case _                                      => None
  }

  private def resourceUpdate(header: HttpHeader): Option[Long] = header.lowercaseName match {
    case "if-match" => try { Some(header.value.toLong) } catch { case _ => None }
    case _          => None
  }

  // TODO:  If-None-Match could contain a list of revisions.
  private def ifNoneMatch(header: HttpHeader): Option[String] = header.lowercaseName match {
    case "if-none-match" => Some(header.value)
    case _               => None
  }

  private def resourceToCrag(resourceDisjunction: Disj[CragResource], cragName: String) = {
    resourceDisjunction >>= { resource: CragResource =>
      Crag(cragName, resource.title)
    }
  }

  private def handleActionFailure(f: ActionFailure) = f match {
    case ValidationError() => HttpResponse(StatusCodes.BadRequest)
    case EditConflict()    => HttpResponse(StatusCodes.PreconditionFailed)
    case NotFound()        => HttpResponse(StatusCodes.NotFound)
    case NotImplemented()  => HttpResponse(StatusCodes.NotImplemented)
  }

  private lazy val slug = "[a-zA-Z0-9_-]+".r
}
  
private case class ETag(override val value: String) extends RawHeader("ETag", value)

