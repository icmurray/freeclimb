package freeclimb.sql

import java.sql.Connection

import freeclimb.api._
import freeclimb.common._
import freeclimb.models.Revisioned

/**
 * Defines common CRUD functions on a repository
 */
trait Repository[M] {

  def create (m: M):             ApiUpdateAction[Revisioned[M]]
  def update (m: Revisioned[M]): ApiUpdateAction[Revisioned[M]]
  def delete (m: Revisioned[M]): ApiUpdateAction[Revisioned[M]]
  def purge  (m: Revisioned[M]): ApiUpdateAction[Revisioned[M]]
}
