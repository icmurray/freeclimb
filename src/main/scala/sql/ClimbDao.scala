package freeclimb.sql

import freeclimb.models._

/**
 * The Data Access Object for Climbs.
 *
 * Defines lower-level domai-model access for Climbs.  Mostly CRUD.
 */
trait ClimbDao extends Repository[Climb] {
  def get(name: String): DB[Option[Climb]]
}
