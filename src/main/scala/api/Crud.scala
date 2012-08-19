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
  def createClimb(climb: Climb): ResultOrCA[Climb]
  def updateClimb(climb: Revisioned[Climb]): ResultOrCA[Climb]
  def deleteClimb(climb: Revisioned[Climb]): SuccessOrCA[Climb]
  def getClimb(name: String): Option[Climb]

}
