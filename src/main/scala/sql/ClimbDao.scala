package freeclimb.sql

import anorm._

import freeclimb.api._
import freeclimb.models._

/**
 * The Data Access Object for Climbs.
 *
 * Defines lower-level domai-model access for Climbs.  Mostly CRUD.
 */
trait ClimbDao extends Repository[Climb] {
  def get(name: String): Action[Option[Revisioned[Climb]], TransactionReadUncommitted] = Action { session =>
    implicit val connection = session.dbConnection
    val result: Boolean = SQL("Select 1").execute()
    None
  }
}

