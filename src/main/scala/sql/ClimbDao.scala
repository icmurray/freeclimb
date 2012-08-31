package freeclimb.sql

import anorm._

import freeclimb.models._

/**
 * The Data Access Object for Climbs.
 *
 * Defines lower-level domai-model access for Climbs.  Mostly CRUD.
 */
trait ClimbDao extends Repository[Climb] {
  def get(name: String): DB[Option[Climb]] = DB { implicit connection =>
    val result: Boolean = SQL("Select 1").execute()
    None
  }
}

