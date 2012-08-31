package freeclimb.sql

import java.sql.Connection

import freeclimb.api._
import freeclimb.common._
import freeclimb.models.Revisioned

/**
 * Defines common CRUD functions on a repository
 */
trait Repository[M] {

  def create(m: M):             DisjunctionT[ApiAction, ConcurrentAccess, Revisioned[M]]
  def update(m: Revisioned[M]): DisjunctionT[ApiAction, ConcurrentAccess, Revisioned[M]]
  def delete(m: Revisioned[M]): DisjunctionT[ApiAction, ConcurrentAccess, Unit]

}
