package freeclimb.sql

import java.sql.Connection

import anorm._

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.common._
import freeclimb.models._

/**
 * The Data Access Object for Crags.
 *
 * Defines lower-level domai-model access for Crags.  Mostly CRUD.
 */
trait CragDao extends Repository[Crag] {

  /** TODO: SQL exception handling. */
  override def create(crag: Crag) = ApiAction { session =>
    implicit val connection = session.dbConnection

    val revision: Option[Long] = SQL(
      """
      INSERT INTO crags(name, title)
        VALUES ({name}, {title})
      """
    ).on("name"  -> crag.name,
         "title" -> crag.title
    ).executeInsert()

    revision match {
      case Some(rev) => new Revisioned[Crag](rev, crag).right
      case None      => ConcurrentUpdate().left
    }
  }

  def get(name: String): ApiAction[Option[Revisioned[Crag]]] = ApiAction { session =>
    implicit val connection = session.dbConnection
    val result: Boolean = SQL("Select 1").execute()
    None
  }

  private implicit def action2EitherT[A,B](
    action: ApiAction[Disjunction[A,B]]): DisjunctionT[ApiAction, A, B] = EitherT(action)
}

