package freeclimb.api

import freeclimb.common._
import freeclimb.models._
import freeclimb.sql._

import scalaz._
import Scalaz._

trait CrudApi {

  val climbDao: ClimbDao

  /**
   * Climb related actions
   */
  def createClimb(climb: Climb): ActionResult[Climb] = climbDao.create(climb)
  def updateClimb(climb: Revisioned[Climb]): ActionResult[Climb] = climbDao.update(climb)
  def deleteClimb(climb: Revisioned[Climb]): ActionSuccess[Climb]
  def getClimb(name: String): ApiAction[Option[Revisioned[Climb]]]

  /**
   * Crag related actions
   */
  def createCrag(crag: Crag): ActionResult[Crag]
  def updateCrag(crag: Revisioned[Crag]): ActionResult[Crag]
  def deleteCrag(crag: Revisioned[Crag]): ActionSuccess[Crag]
  def getCrag(name: String): ApiAction[Option[Crag]]

  // This is only here to check that I've setup the ApiAction's implicit
  // functor and monad instances correctly.  I'll remove it later.
  private def createAndDelete(climb: Climb) = for {
    created <- createClimb(climb)
  } yield deleteClimb(created)

  // Again, this is onyl really here as a check that it works.
  private def createUpdateThenDelete(climb: Climb): ActionSuccess[Climb] = for {
    created <- climbDao.create(climb)
    updated <- climbDao.update(created)
    updatedAgain <- updateClimb(updated)
  } yield climbDao.delete(updated)

}
