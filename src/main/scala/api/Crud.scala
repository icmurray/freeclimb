package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._
import freeclimb.sql._

trait CrudApi {

  val climbDao: ClimbDao

  /**
   * Climb related actions
   */
  def createClimb(climb: Climb): ActionResult[Climb] = ApiAction { session =>
    climbDao.create(climb)(session.dbConnection)
  }

  def updateClimb(climb: Revisioned[Climb]): ActionResult[Climb]
  def deleteClimb(climb: Revisioned[Climb]): ActionSuccess[Climb]
  def getClimb(name: String): ApiAction[Option[Revisioned[Climb]]]

  /**
   * Crag related actions
   */
  def createCrag(crag: Crag): ActionResult[Crag]
  def updateCrag(crag: Revisioned[Crag]): ActionResult[Crag]
  def deleteCrag(crag: Revisioned[Crag]): ActionSuccess[Crag]
  def getCrag(name: String): ApiAction[Option[Crag]]

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
