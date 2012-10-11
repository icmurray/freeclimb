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

trait Dao {

  protected def trySql[A](body: => \/[ActionFailure,A]): Catchable[A] = new Catchable[A] {
    def catchSqlState(errorHandler: PartialFunction[SqlError.SqlError, ActionFailure]) = {
      try {
        body
      } catch {
        case e: SQLException => e.sqlError map errorHandler match {
          case Some(b) => b.left
          case None    => throw e
        }
        case e    => throw e
      }
    }
  }

  /**
   * Anorm Parsers
   */

  protected def climb(climbTable: String, cragTable: String) = {
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

  protected def grade = {
    str("grading_system") ~ int("difficulty") map { case system~difficulty =>
      Grade(system, difficulty)
    }
  }

  protected def crag(table: String) = {
    str(table + ".name") ~
    str(table + ".title") map {
      case name~title => Crag.makeUnsafe(name, title)
    }
  }

  protected def revisionedClimb(climbTable: String, cragTable: String) = {
    climb(climbTable, cragTable) ~ int(climbTable + ".revision") map {
      case climb~revision => Revisioned[Climb](revision, climb)
    }
  }

  protected def revisionedCrag(table: String) = {
    crag(table) ~ int(table + ".revision") map {
      case crag~revision => Revisioned[Crag](revision, crag)
    }
  }


}

trait Catchable[A] {
  def catchSqlState(errorHandler: PartialFunction[SqlError.SqlError, ActionFailure]): \/[ActionFailure,A]
}
