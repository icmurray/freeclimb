package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._

trait CrudApi {

  /**
   * Climb related actions
   */
  def createClimb(climb: Climb): ActionResult[Climb]
  def updateClimb(climb: Revisioned[Climb]): ActionResult[Climb]
  def deleteClimb(climb: Revisioned[Climb]): ActionSuccess[Climb]
  def getClimb(name: String): ApiAction[Option[Climb]]

  def createCrag(crag: Crag): ActionResult[Crag]

  // This is here to demonstrate that by ConcurrentAccess taking a type
  // parameter, and composing two ActionResults about Climbs and Crags together
  // that you get an unusual return type.
  def createClimbAndCrag(
      climb: Climb,
      crag: Crag): DisjunctionT[ApiAction, ConcurrentAccess[_ >: Climb with Crag <: ScalaObject], Revisioned[Climb]] = for {
    createdClimb <- createClimb(climb)
    createdCrag <- createCrag(crag)
  } yield createdClimb

  // This is only here to check that I've setup the ApiAction's implicit
  // functor and monad instances correctly.  I'll remove it later.
  def createAndDelete(climb: Climb) = for {
    created <- createClimb(climb)
  } yield deleteClimb(created)

}
