package freeclimb.api

import freeclimb.common._
import freeclimb.models._
import freeclimb.sql._

import scalaz._
import Scalaz._

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
  //def getClimb(name: String)                = climbDao.get(name)

  // These are here to check that the isolation level checking works.
  // This one should compile:
  def runCreateClimbWorks(climb: Climb)(session: DbSession[TransactionSerializable]): Disjunction[ActionFailure, Revisioned[Climb]] =
    createClimb(climb).runInTransaction(session)

  // As should this one:
  def runCreateClimbWorksToo(climb: Climb)(session: DbSession[TransactionRepeatableRead]): Disjunction[ActionFailure, Revisioned[Climb]] =
    createClimb(climb).runInTransaction(session)

  // And when uncommented, this one shouldn't:
  //def runCreateClimbShouldNotWork(climb: Climb)(session: DbSession[TransactionNone]): Disjunction[ActionFailure, Revisioned[Climb]] =
  //  createClimb(climb).runInTransaction(session)


  /**
   * Crag related actions.  Again, these just pass straight through to the DAO
   * layer.
   */
  def createCrag(crag: Crag)             = cragDao.create(crag)
  def updateCrag(crag: Revisioned[Crag]) = cragDao.update(crag)
  def deleteCrag(crag: Revisioned[Crag]) = cragDao.delete(crag)
  def getCrag(name: String)              = cragDao.get(name)

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
