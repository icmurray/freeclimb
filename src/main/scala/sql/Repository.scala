package freeclimb.sql

import java.sql.Connection

import freeclimb.api._
import freeclimb.models._

/**
 * Defines common CRUD functions on a repository
 */
trait Repository[M] {
  def create(m: M): DB[Disjunction[ConcurrentAccess[M], Revisioned[M]]]
}
