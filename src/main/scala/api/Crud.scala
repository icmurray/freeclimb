package freeclimb.api

import freeclimb.common._
import freeclimb.models._
import freeclimb.sql._

import scalaz._
import Scalaz._

object CrudApi extends CrudApi {
  override val climbDao = ClimbDao
  override val cragDao = CragDao
}

trait CrudApi {

  /**
   * Abstract members.
   */
  val climbDao: ClimbDao
  val cragDao: CragDao

  /**
   * Climb related actions.  These just pass straight through to the DAO
   * layer.
   */
  def createClimb(climb: Climb)             = climbDao.create(climb)
  def updateClimb(climb: Revisioned[Climb]) = climbDao.update(climb)
  def deleteClimb(climb: Revisioned[Climb]) = climbDao.delete(climb)
  def getClimb(crag: String, climb: String) = climbDao.get(crag, climb)
  def getClimbOption(crag: String, climb: String) = climbDao.getOption(crag, climb)

  // These are here to check that the isolation level checking works.

  // This is a combined action with type inference:
  def createUpdateThenGet(climb: Climb) = for {
    cragRev  <- cragDao.create(climb.crag)
    climbRev <- climbDao.create(climb)
    _        <- climbDao.update(climbRev)
    result   <- climbDao.get(climb.crag.name, climb.name)
  } yield result

  // This is a combined action with explicit type
  def createUpdateThenGetExplicit(climb: Climb): ApiUpdateAction[Revisioned[Climb]]  = for {
    cragRev  <- cragDao.create(climb.crag)
    climbRev <- climbDao.create(climb)
    _        <- climbDao.update(climbRev)
    result   <- climbDao.get(climb.crag.name, climb.name)
  } yield result

  /**
   * Crag related actions.  Again, these just pass straight through to the DAO
   * layer.
   */
  def createCrag(crag: Crag)             = cragDao.create(crag)
  def updateCrag(crag: Revisioned[Crag]) = cragDao.update(crag)
  def deleteCrag(crag: Revisioned[Crag]) = cragDao.delete(crag)
  def getCrag(name: String)              = cragDao.get(name)
  def getCragOption(name: String)        = cragDao.getOption(name)

  def updateCrag(crag: Revisioned[Crag], name: String): ApiUpdateAction[Revisioned[Crag]] = {
    if (name == crag.model.name) {
      updateCrag(crag)
    } else {
      ApiUpdateAction(_ => NotImplemented().left)
    }
  }

  // This is only here to check that I've setup the Action's implicit
  // functor and monad instances correctly.  I'll remove it later.
  private def createAndDelete(climb: Climb) = for {
    created <- createClimb(climb)
  } yield deleteClimb(created)

  // Again, this is onyl really here as a check that it works.
  private def createUpdateThenDelete(climb: Climb) = for {
    created <- climbDao.create(climb)
    updated <- climbDao.update(created)
    updatedAgain <- updateClimb(updated)
  } yield climbDao.delete(updated)

}
