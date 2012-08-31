package freeclimb.sql

import anorm._

import freeclimb.api._
import freeclimb.models._

/**
 * The Data Access Object for Crags.
 *
 * Defines lower-level domai-model access for Crags.  Mostly CRUD.
 */
trait CragDao extends Repository[Crag] {
  def get(name: String): ApiAction[Option[Revisioned[Crag]]] = ApiAction { session =>
    implicit val connection = session.dbConnection
    val result: Boolean = SQL("Select 1").execute()
    None
  }
}

