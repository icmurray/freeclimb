package freeclimb.sql

import java.sql.{SQLException, Connection}

import anorm._
import anorm.SqlParser._

import org.postgresql.util.PGobject

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql.SqlError._

/**
 * The Data Access Object for Climbs.
 *
 * Defines lower-level domai-model access for Climbs.  Mostly CRUD.
 */
object ClimbDao extends ClimbDao

trait ClimbDao extends Repository[Climb] {

  def get(crag: String, climb: String): ApiReadAction[Revisioned[Climb]] = for {
    optionRev <- getOption(crag, climb)
    result <- optionRev match {
      case None      => ApiReadAction.pure(NotFound().left)
      case Some(rev) => ApiReadAction.pure(rev.right)
    }
  } yield result

  def getOption(crag: String, climb: String): ApiReadAction[Option[Revisioned[Climb]]] = ApiReadAction { session =>
    implicit val connection = session.dbConnection

    SQL(
      """
      SELECT climbs.name,
             climbs.title,
             climbs.description,
             climbs.revision,
             crags.name,
             crags.title,
             grades.grading_system::varchar,
             grades.difficulty FROM climbs
      INNER JOIN crags ON climbs.crag_id = crags.id
      INNER JOIN grades ON climbs.grade_id = grades.id
      WHERE crags.name = {crag_name}
        AND climbs.name = {climb_name}
      """
    ).on(
      "crag_name" -> crag,
      "climb_name" -> climb
    ).as(revisionedClimb.singleOpt).right
  }

  override def create(climb: Climb) = ApiAction { session =>
    implicit val connection = session.dbConnection

    try {
      
      val nextRevision: Long = SQL("SELECT nextval('revision_seq');").as(scalar[Long].single)

      SQL(
        """
        INSERT INTO climbs (
          name, 
          title,
          description,
          crag_id,
          revision,
          grade_id)
        VALUES (
          {name},
          {title},
          {description},
          (SELECT id FROM crags WHERE name = {crag_name}),
          {revision},
          (SELECT id FROM grades WHERE grading_system = {grading_system}::GradingSystem
                                   AND difficulty = {difficulty})
        );
        """
      ).on("name"               -> climb.name,
           "title"              -> climb.title,
           "description"        -> climb.description,
           "crag_name"          -> climb.crag.name,
           "revision"           -> nextRevision,
           "grading_system"     -> climb.grade.system.toString,
           "difficulty"         -> climb.grade.difficulty
      ).executeInsert()

      Revisioned[Climb](nextRevision, climb).right
    } catch {
      case e: SQLException => e.sqlError match {
        case Some(UniqueViolation)  => EditConflict().left
        case Some(NotNullViolation) => ValidationError().left
        case _                      => throw e
      }
      case e => throw e
    }
  }

  override def update(climbRev: Revisioned[Climb]) = ApiAction { session =>
    implicit val connection = session.dbConnection
    try {

      get(climbRev.model.crag.name, climbRev.model.name).runWith(session) fold (
        error => error.left,
        currentRevision => currentRevision match {
          case latest if latest.revision != climbRev.revision => EditConflict().left
          case _ =>
            val climb = climbRev.model
            val nextRevision: Long = SQL("SELECT nextval('revision_seq');").as(scalar[Long].single)

            SQL(
              """
              UPDATE climbs SET
                title = {title},
                description = {description},
                grade_id = (SELECT id FROM grades
                              WHERE grading_system = {grading_system}::GradingSystem
                                AND difficulty = {difficulty}),
                revision = {revision}
              FROM crags
              WHERE climbs.name = {name}
                AND crags.name = {crag_name}
                AND climbs.crag_id = crags.id
              """
            ).on(
              "title"          -> climb.title,
              "description"    -> climb.description,
              "revision"       -> nextRevision,
              "name"           -> climb.name,
              "crag_name"      -> climb.crag.name,
              "grading_system" -> climb.grade.system.toString,
              "difficulty"     -> climb.grade.difficulty
            ).execute()
            new Revisioned[Climb](nextRevision, climb).right
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

  override def delete(climbRev: Revisioned[Climb]) = ApiAction { session =>
    implicit val connection = session.dbConnection
    try {

      get(climbRev.model.crag.name, climbRev.model.name).runWith(session).fold (
        error => error.left,
        currentRevision => currentRevision match {
          case latest if latest.revision != climbRev.revision => EditConflict().left
          case _ =>
            val climb = climbRev.model

            SQL(
              """
              DELETE from climbs
                WHERE name = {name}
                  AND crag_id = (SELECT id FROM crags WHERE name = {crag_name})
              """
            ).on(
              "name"      -> climb.name,
              "crag_name" -> climb.crag.name
            ).execute()
            climbRev.right
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

  override def purge(climb: Revisioned[Climb]) = ApiAction { session =>
    NotImplemented().left
  }

  private lazy val climb = {
    grade ~
    crag ~
    str("climbs.name") ~
    str("climbs.title") ~
    str("climbs.description") map {
      case grade~crag~name~title~description => Climb.makeUnsafe(
        name,
        title,
        description,
        crag,
        grade
      )
    }
  }

  private lazy val grade = {
    str("grading_system") ~ int("difficulty") map { case system~difficulty =>
      Grade(system, difficulty)
    }
  }

  private lazy val crag = {
    str("crags.name") ~
    str("crags.title") map {
      case name~title => Crag.makeUnsafe(name, title)
    }
  }

  private lazy val revisionedClimb = {
    climb ~ int("climbs.revision") map {
      case climb~revision => Revisioned[Climb](revision, climb)
    }
  }

}

