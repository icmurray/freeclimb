package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._

trait CrudApi {


  /**
   * Climb related actions
   */
  def createClimb(climb: Climb)(implicit session: ApiSession): ResultOrCA[Climb]
  def updateClimb(climb: Revisioned[Climb])(implicit session: ApiSession): ResultOrCA[Climb]
  def deleteClimb(climb: Revisioned[Climb])(implicit session: ApiSession): SuccessOrCA[Climb]
  def getClimb(name: String)(implicit session: ApiSession): Option[Climb]

}
