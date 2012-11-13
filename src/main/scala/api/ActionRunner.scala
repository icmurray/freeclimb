package freeclimb.api

import javax.sql.DataSource

import scalaz._
import Scalaz._

import freeclimb.sql.IsolationLevel

trait ActionRunner {
  def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](action: ActionT[M,A,I,W])(implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A]
}

class DefaultActionRunner(private val dataSource: DataSource) extends ActionRunner {

  // TODO: decide whether these should be callbacks, or some sort of Listener
  private var callbacks: List[PartialFunction[ActionEvent, Unit]] = Nil

  def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](action: ActionT[M,A,I,W])(implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A] = {
    val connection = dataSource.getConnection()
    val isolationLevel: I = m.erasure.newInstance.asInstanceOf[I]
    connection.setAutoCommit(false)
    connection.setTransactionIsolation(isolationLevel.jdbcLevel)
    val session = new DbSession[I] { val dbConnection = connection }
    try {
      val result = action(session)
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
      connection.close()
    }
  }

  def processActionEvents(events: List[ActionEvent]) {
    for {
      event <- events
      callback <- callbacks
      if callback isDefinedAt event
    } callback(event)
  }

  def subscribe(callback: PartialFunction[ActionEvent, Unit]) = {
    this.synchronized {
      callbacks = callback :: callbacks
    }
  }

}
