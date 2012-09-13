package freeclimb.sql

import java.sql.{SQLException, Connection}

import anorm._
import anorm.SqlParser._

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.common._
import freeclimb.models._

import freeclimb.sql.SqlError._

object CragDao extends CragDao

/**
 * The Data Access Object for Crags.
 *
 * Defines lower-level domai-model access for Crags.  Mostly CRUD.
 */
trait CragDao extends Repository[Crag] {

  override def create(crag: Crag) = ActionT { session =>
    implicit val connection = session.dbConnection

    try{

      val nextRevision: Long = SQL("SELECT nextval('crag_revision_seq');").as(scalar[Long].single)

      SQL(
        """
        INSERT INTO crags(name, title, revision)
          VALUES ({name}, {title}, {revision});
        """
      ).on("name"     -> crag.name,
           "title"    -> crag.title,
           "revision" -> nextRevision
      ).executeInsert()

      Revisioned[Crag](nextRevision, crag).right

    } catch {
      case e: SQLException => e.sqlError match {
        case Some(UniqueViolation) => connection.rollback() ; ConcurrentUpdate().left
        case _                     => connection.rollback() ; throw e
      }
      case e => connection.rollback() ; throw e
    }

  }

  override def delete(cragRev: Revisioned[Crag]) = ActionT { session =>

    implicit val connection = session.dbConnection
    try {

      val currentRevision: Option[Revisioned[Crag]] = get(cragRev.model.name).runWith(session)
      currentRevision match {
        case None => ConcurrentUpdate().left
        case Some(latest) if latest.revision != cragRev.revision => ConcurrentUpdate().left
        case _ =>
          val crag = cragRev.model

          SQL(
            """
            DELETE from crags WHERE name = {name}
            """
          ).on(
            "name"     -> crag.name
          ).execute()
          cragRev.right
      }
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) => connection.rollback() ; ConcurrentUpdate().left
        case _                          => connection.rollback() ; throw e
      }
      case e                            => connection.rollback() ; throw e
    }
  }

  override def purge(cragRev: Revisioned[Crag]) = ActionT { session =>

    implicit val connection = session.dbConnection
    try {

      val currentRevision: Option[Revisioned[Crag]] = get(cragRev.model.name).runWith(session)
      currentRevision match {
        case None => ConcurrentUpdate().left
        case Some(latest) if latest.revision != cragRev.revision => ConcurrentUpdate().left
        case _ =>
          val cragId = SQL(
            """
            SELECT crag_id FROM crag_history
              WHERE name = {name}
                AND revision = {revision}
            """
          ).on(
            "name"     -> cragRev.model.name,
            "revision" -> cragRev.revision
          ).as(scalar[Long].single)

          SQL(
            """
            DELETE from crag_history
              WHERE crag_id = {crag_id};
            DELETE FROM crags
              WHERE id = {crag_id}
            """
          ).on(
            "crag_id" -> cragId
          ).execute()
          cragRev.right
      }
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) => connection.rollback() ; ConcurrentUpdate().left
        case _                          => connection.rollback() ; throw e
      }
      case e                            => connection.rollback() ; throw e
    }
  }

  override def update(cragRev: Revisioned[Crag]) = ActionT { session =>
    implicit val connection = session.dbConnection
    try {

      val currentRevision: Option[Revisioned[Crag]] = get(cragRev.model.name).runWith(session)
      currentRevision match {
        case None => ConcurrentUpdate().left
        case Some(latest) if latest.revision != cragRev.revision => ConcurrentUpdate().left
        case _ =>
          val crag = cragRev.model
          val nextRevision: Long = SQL("SELECT nextval('crag_revision_seq');").as(scalar[Long].single)

          SQL(
            """
            UPDATE crags SET
              title = {title},
              revision = {revision}
            WHERE name = {name}
            """
          ).on(
            "title"    -> crag.title,
            "revision" -> nextRevision,
            "name"     -> crag.name
          ).execute()
          new Revisioned[Crag](nextRevision, crag).right
      }
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) => connection.rollback() ; ConcurrentUpdate().left
        case _                          => connection.rollback() ; throw e
      }
      case e                            => connection.rollback() ; throw e
    }

  }

  def get(name: String): Action[Option[Revisioned[Crag]], TransactionReadCommitted] = Action { session =>
    implicit val connection = session.dbConnection

    SQL(
      """
      SELECT name, title, revision FROM crags
      WHERE name = {name}
      """
    ).on(
      "name" -> name
    ).as(revisionedCrag.singleOpt)
  }

  def history(crag: Crag): Action[Seq[Revisioned[Crag]], TransactionReadCommitted] = Action { session =>
    implicit val connection = session.dbConnection
    SQL(
      """
      SELECT h.name, h.title, h.revision FROM crag_history AS h
        INNER JOIN crags AS c ON c.id = h.crag_id
        WHERE c.name = {name}
        ORDER BY h.revision DESC
      """
    ).on(
      "name" -> crag.name
    ).as(revisionedCrag *)
  }

  private val crag = {
    str("name") ~
    str("title") map {
      case name~title => Crag.makeUnsafe(name, title)
    }
  }

  private val revisionedCrag = {
    crag ~ int("revision") map {
      case crag~revision => Revisioned[Crag](revision, crag)
    }
  }

}

