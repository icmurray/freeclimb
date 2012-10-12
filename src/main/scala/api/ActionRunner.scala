package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.sql.IsolationLevel

object DefaultActionRunner extends ActionRunner
                              with NoOpEventProcessor

object NotifyingActionRunner extends ActionRunner
                                with SynchronousNotifications

trait ActionRunner {

  def runInTransaction[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](s: DbSession[I])(action: ActionT[M,A,I,W])(implicit F: Failable[M[_]], M: Functor[M]): M[A] = {
    val connection = s.dbConnection
    try {
      connection.setAutoCommit(false)
      connection.setTransactionIsolation(s.jdbcLevel)
      val result = action(s)
      if (F.isFailure(result)) {
        connection.rollback()
      } else {
        connection.commit()
      }

      // process the logged events
      result map { _._1 } map processActionEvents

      // Discard the logged events from the final result
      result map { _._2 }
    } catch {
      case e => connection.rollback() ; throw e
    } finally {
      connection.setAutoCommit(true)
      connection.close()
    }
  }

  def processActionEvents(events: List[ActionEvent]): Unit

}

trait NoOpEventProcessor {
  def processActionEvents(events: List[ActionEvent]) {}
}

/**
 * TODO: ensure thread-safety
 */
trait SynchronousNotifications {
  private var callbacks: List[PartialFunction[ActionEvent, Unit]] = Nil

  def subscribeSynchronously(callback: PartialFunction[ActionEvent, Unit]) = {
    callbacks = callback :: callbacks
  }

  def processActionEvents(events: List[ActionEvent]) {
    for {
      event <- events
      callback <- callbacks
      if callback isDefinedAt event
    } callback(event)
  }
}
