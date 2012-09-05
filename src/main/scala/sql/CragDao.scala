package freeclimb.sql

import java.sql.{SQLException, Connection}

import anorm._

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.common._
import freeclimb.models._

import freeclimb.sql.SqlError._

/**
 * The Data Access Object for Crags.
 *
 * Defines lower-level domai-model access for Crags.  Mostly CRUD.
 */
trait CragDao extends Repository[Crag] {

  /** TODO: SQL exception handling. */
  override def create(crag: Crag) = ActionT { session =>
    implicit val connection = session.dbConnection

    try{

      val revision: Option[Long] = SQL(
        """
        INSERT INTO crags(name, title)
          VALUES ({name}, {title});
        """
      ).on("name"  -> crag.name,
           "title" -> crag.title
      ).executeInsert()

      revision match {
        case Some(rev) => new Revisioned[Crag](rev, crag).right
        case None      => ConcurrentUpdate().left
      }

    } catch {
      case e: SQLException => e.sqlError match {
        case Some(UniqueViolation) => connection.rollback() ; ConcurrentUpdate().left
        case _                     => connection.rollback() ; throw e
      }
      case e => connection.rollback() ; throw e
    }

  }

  def get(name: String): Action[Option[Revisioned[Crag]], TransactionReadCommitted] = Action { session =>
    implicit val connection = session.dbConnection
    val result: Boolean = SQL("Select 1").execute()
    None
  }

}

