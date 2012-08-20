package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._

trait CrudApi {

  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type ResultOrCA[T] = Validation[ConcurrentAccess[T], Revisioned[T]]
  type SuccessOrCA[T] = Validation[ConcurrentAccess[T], Unit]

  /**
   * Climb related actions
   */
  def createClimb(climb: Climb)(implicit session: ApiSession): ResultOrCA[Climb]
  def updateClimb(climb: Revisioned[Climb])(implicit session: ApiSession): ResultOrCA[Climb]
  def deleteClimb(climb: Revisioned[Climb])(implicit session: ApiSession): SuccessOrCA[Climb]
  def getClimb(name: String)(implicit session: ApiSession): Option[Climb]

}
