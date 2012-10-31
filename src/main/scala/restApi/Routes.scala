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
  protected val source: DataSource

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

  private def runRead[A](action: => ApiReadAction[A]) = {
    NotifyingActionRunner.runInTransaction(newReadSession)(action)
  }

  private def newReadSession = new DbSession[TransactionReadCommitted] {
    override lazy val dbConnection = {
      val c = source.getConnection()
      c.setAutoCommit(false)
      c.setTransactionIsolation(TransactionReadCommitted.jdbcLevel)
      c
    }
  }

  private def runUpdate[A](action: => ApiUpdateAction[A]) = {
    NotifyingActionRunner.runInTransaction(newUpdateSession)(action)
  }

  private def newUpdateSession = new DbSession[TransactionRepeatableRead] {
    override lazy val dbConnection = {
      val c = source.getConnection()
      c.setAutoCommit(false)
      c.setTransactionIsolation(TransactionRepeatableRead.jdbcLevel)
      c
    }
  }

  private lazy val slug = "[a-zA-Z0-9_-]+".r
}
