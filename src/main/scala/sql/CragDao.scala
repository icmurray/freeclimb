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

  override def create(crag: Crag) = ApiAction { session =>
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
        case Some(UniqueViolation) => EditConflict().left
        case _                     => throw e
      }
      case e => throw e
    }

  }

  override def delete(cragRev: Revisioned[Crag]) = ApiAction { session =>

    implicit val connection = session.dbConnection
    try {

      get(cragRev.model.name).runWith(session).fold (
        error => error.left,
        currentRevision => currentRevision match {
          case latest if latest.revision != cragRev.revision => EditConflict().left
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
      )
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) =>  EditConflict().left
        case _                          =>  throw e
      }
      case e                            =>  throw e
    }
  }

  override def purge(cragRev: Revisioned[Crag]) = ApiAction { session =>

    implicit val connection = session.dbConnection
    try {

      get(cragRev.model.name).runWith(session).fold (
        error => error.left,
        currentRevision => currentRevision match {

          case latest if latest.revision != cragRev.revision => EditConflict().left
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
      )
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) =>  EditConflict().left
        case _                          =>  throw e
      }
      case e                            =>  throw e
    }
  }

  override def update(cragRev: Revisioned[Crag]) = ApiAction { session =>
    implicit val connection = session.dbConnection
    try {

      get(cragRev.model.name).runWith(session) fold (
        error => error.left,
        currentRevision => currentRevision match {
          case latest if latest.revision != cragRev.revision => EditConflict().left
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
      )
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(SerializationFailure) =>  EditConflict().left
        case _                          =>  throw e
      }
      case e                            =>  throw e
    }

  }

  def get(name: String): ApiReadAction[Revisioned[Crag]] = for {
    optionRev <- getOption(name)
    result <- optionRev match {
      case None      => ApiReadAction.pure(NotFound().left)
      case Some(rev) => ApiReadAction.pure(rev.right)
    }
  } yield result

  def getOption(name: String): ApiReadAction[Option[Revisioned[Crag]]] = ApiReadAction { session =>
    implicit val connection = session.dbConnection

    SQL(
      """
      SELECT name, title, revision FROM crags
      WHERE name = {name}
      """
    ).on(
      "name" -> name
    ).as(revisionedCrag.singleOpt).right
  }

  def history(crag: Crag): ApiReadAction[Seq[Revisioned[Crag]]] = ApiReadAction { session =>
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
    ).as(revisionedCrag *).right
  }

  def deletedList(): ApiReadAction[Seq[Revisioned[Crag]]] = ApiReadAction { session =>
    implicit val connection = session.dbConnection
    SQL(
      """
      SELECT DISTINCT ON (h.crag_id)
             h.name, h.title, h.revision FROM crag_history as h
        LEFT OUTER JOIN crags AS c ON c.id = h.crag_id
        WHERE c.id IS NULL
        ORDER BY h.crag_id, h.revision DESC
      """
    ).as(revisionedCrag *).right
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

