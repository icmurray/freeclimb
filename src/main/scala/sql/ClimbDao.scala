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
    ).as(revisionedClimb("climbs", "crags").singleOpt).right
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

      val created = Revisioned[Climb](nextRevision, climb)
      (List(ClimbCreated(created)), created).right
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
          case (_,latest) if latest.revision != climbRev.revision => EditConflict().left
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
            val updated = Revisioned[Climb](nextRevision, climb)
            (List(ClimbUpdated(updated)), updated).right
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
          case (_, latest) if latest.revision != climbRev.revision => EditConflict().left
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
            val deleted = climbRev
            (List(ClimbDeleted(deleted)), deleted).right
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

  override def purge(climbRev: Revisioned[Climb]) = ApiAction { session =>
    implicit val connection = session.dbConnection
    try {

      get(climbRev.model.crag.name, climbRev.model.name).runWith(session).fold (
        error => error.left,
        currentRevision => currentRevision match {

          case (_, latest) if latest.revision != climbRev.revision => EditConflict().left
          case _ =>
            val climbId = SQL(
              """
              SELECT climb_id FROM climb_history
                WHERE name = {name}
                  AND revision = {revision}
              """
            ).on(
              "name"     -> climbRev.model.name,
              "revision" -> climbRev.revision
            ).as(scalar[Long].single)

            SQL(
              """
              DELETE from climb_history
                WHERE climb_id = {climb_id};
              DELETE FROM climbs
                WHERE id = {climb_id}
              """
            ).on(
              "climb_id" -> climbId
            ).execute()
            val purged = climbRev
            (List(ClimbDeleted(purged)), purged).right
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

  def history(climb: Climb): ApiReadAction[Seq[Revisioned[Climb]]] = ApiReadAction { session =>
    implicit val connection = session.dbConnection
    SQL(
      """
      SELECT climb_history.name,
             climb_history.title,
             climb_history.description,
             climb_history.revision,
             crags.name,
             crags.title,
             grades.grading_system::varchar,
             grades.difficulty FROM climb_history
      INNER JOIN climbs ON climb_history.climb_id = climbs.id
      INNER JOIN crags ON climb_history.crag_id = crags.id
      INNER JOIN grades ON climb_history.grade_id = grades.id
      WHERE crags.name = {crag_name}
        AND climb_history.name = {climb_name}
      ORDER BY climb_history.revision DESC
      """
    ).on(
      "climb_name" -> climb.name,
      "crag_name"  -> climb.crag.name
    ).as(revisionedClimb("climb_history", "crags") *).right
  }

  def deletedList(): ApiReadAction[Seq[Revisioned[Climb]]] = ApiReadAction { session =>
    implicit val connection = session.dbConnection
    SQL(
      """
      SELECT DISTINCT ON (climb_history.climb_id, crag_history.crag_id)
             climb_history.name,
             climb_history.title,
             climb_history.description,
             climb_history.revision,
             crag_history.name,
             crag_history.title,
             grades.grading_system::varchar,
             grades.difficulty FROM climb_history
        LEFT OUTER JOIN climbs ON climbs.id = climb_history.climb_id
        INNER JOIN crag_history ON climb_history.crag_id = crag_history.crag_id
        INNER JOIN grades ON climb_history.grade_id = grades.id
        WHERE climbs.id IS NULL
        ORDER BY climb_history.climb_id, crag_history.crag_id,
                 climb_history.revision DESC,
                 crag_history.revision DESC
      """
    ).as(revisionedClimb("climb_history", "crag_history") *).right
  }

  def climbsCreatedOrUpdatedSince(crag: Revisioned[Crag]) = ApiReadAction { session =>
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
        AND climbs.revision > {latest_revision}
      """
    ).on(
      "crag_name" -> crag.model.name,
      "latest_revision" -> crag.revision
    ).as(revisionedClimb("climbs", "crags") *).right
  }

  def climbsDeletedSince(crag: Revisioned[Crag]) = ApiReadAction { session =>
    implicit val connection = session.dbConnection
    SQL(
      """
      SELECT DISTINCT ON (climb_history.climb_id)
             climb_history.name,
             climb_history.title,
             climb_history.description,
             climb_history.revision,
             crags.name,
             crags.title,
             grades.grading_system::varchar,
             grades.difficulty FROM climb_history
        LEFT OUTER JOIN climbs ON climbs.id = climb_history.climb_id
        INNER JOIN crags ON climb_history.crag_id = crags.id
        INNER JOIN grades ON climb_history.grade_id = grades.id
        WHERE climbs.id IS NULL
          AND crags.name = {crag_name}
          AND climb_history.revision > {latest_revision}
        ORDER BY climb_history.climb_id,
                 climb_history.revision DESC
      """
    ).on(
      "crag_name" -> crag.model.name,
      "latest_revision" -> crag.revision
    ).as(revisionedClimb("climb_history", "crags") *).right
  }

  private def climb(climbTable: String, cragTable: String) = {
    grade ~
    crag(cragTable) ~
    str(climbTable + ".name") ~
    str(climbTable + ".title") ~
    str(climbTable + ".description") map {
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

  private def crag(table: String) = {
    str(table + ".name") ~
    str(table + ".title") map {
      case name~title => Crag.makeUnsafe(name, title)
    }
  }

  private def revisionedClimb(climbTable: String, cragTable: String) = {
    climb(climbTable, cragTable) ~ int(climbTable + ".revision") map {
      case climb~revision => Revisioned[Climb](revision, climb)
    }
  }

}

