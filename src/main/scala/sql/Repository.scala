package freeclimb.sql

import java.sql.Connection

import freeclimb.api._
import freeclimb.common._
import freeclimb.models.Revisioned

/**
 * Defines common CRUD functions on a repository
 */
trait Repository[M] {

  def create(m: M):             ActionResult[M, TransactionSerializable]
  def update(m: Revisioned[M]): ActionResult[M, TransactionSerializable]
  def delete(m: Revisioned[M]): ActionResult[M, TransactionSerializable]

}
