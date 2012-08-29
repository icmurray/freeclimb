package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._

trait CrudApi {

  /**
   * Climb related actions
   */
  def createClimb(climb: Climb): ResultOrCA[Climb]
  def updateClimb(climb: Revisioned[Climb]): ResultOrCA[Climb]
  def deleteClimb(climb: Revisioned[Climb]): SuccessOrCA[Climb]
  def getClimb(name: String): SessionReader[Option[Climb]]

}
