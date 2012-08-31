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

  // NOTE: I think it'll be necessary to drop the type parameter from
  // `ConcurrentAccess` in order to fix the type on the left of the
  // `Disjunction`.  I think this will be necessary in order that a
  // `ActionResult[Climb]` and `ActionResult[Crag]` can compose.
  type ActionResult[T] = DisjunctionT[ApiAction, ConcurrentAccess[T], Revisioned[T]]
  type ActionSuccess[T] = DisjunctionT[ApiAction, ConcurrentAccess[T], Unit]

  /**
   * Some implicit conversions to make writing actions a bit less verbose
   */
  private implicit def apiAction2EitherT[A,B](action: ApiAction[Disjunction[A,B]]): DisjunctionT[ApiAction,A,B] = EitherT(action)

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
