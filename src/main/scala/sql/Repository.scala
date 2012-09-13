package freeclimb.sql

import java.sql.Connection

import freeclimb.api._
import freeclimb.common._
import freeclimb.models.Revisioned

/**
 * Defines common CRUD functions on a repository
 */
trait Repository[M] {

  def create(m: M):             ActionResult[M, TransactionRepeatableRead]
  def update(m: Revisioned[M]): ActionResult[M, TransactionRepeatableRead]
  def delete(m: Revisioned[M]): ActionResult[M, TransactionRepeatableRead]
  def purge (m: Revisioned[M]): ActionResult[M, TransactionRepeatableRead]

}
