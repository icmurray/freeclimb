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
}

