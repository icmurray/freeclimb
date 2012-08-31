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
  def createClimb(climb: Climb): ActionResult[Climb] = ApiAction { session =>
    climbDao.create(climb).run.runWithinTransaction(session.dbConnection)
  }

  def updateClimb(climb: Revisioned[Climb]): ActionResult[Climb] = ApiAction { session =>
    climbDao.update(climb).run.run(session.dbConnection)
  }

  def deleteClimb(climb: Revisioned[Climb]): ActionSuccess[Climb]
  def getClimb(name: String): ApiAction[Option[Revisioned[Climb]]]

  /**
   * Crag related actions
   */
  def createCrag(crag: Crag): ActionResult[Crag]
  def updateCrag(crag: Revisioned[Crag]): ActionResult[Crag]
  def deleteCrag(crag: Revisioned[Crag]): ActionSuccess[Crag]
  def getCrag(name: String): ApiAction[Option[Crag]]

  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type ActionResult[T] = DisjunctionT[ApiAction, ConcurrentAccess, Revisioned[T]]
  type ActionSuccess[T] = DisjunctionT[ApiAction, ConcurrentAccess, Unit]

  /**
   * Some implicit conversions to make writing actions a bit less verbose
   */
  private implicit def apiAction2EitherT[A,B](action: ApiAction[Disjunction[A,B]]): DisjunctionT[ApiAction,A,B] = EitherT(action)

  // This is only here to check that I've setup the ApiAction's implicit
  // functor and monad instances correctly.  I'll remove it later.
  def createAndDelete(climb: Climb) = for {
    created <- createClimb(climb)
  } yield deleteClimb(created)

}
