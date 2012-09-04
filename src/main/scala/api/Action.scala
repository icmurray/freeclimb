package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._
import freeclimb.sql.IsolationLevel

case class Action[+A, -I <: IsolationLevel](g: DbSession[I] => A) {

  def apply(s: DbSession[I]) = g(s)

  /** Synonym for apply() */
  def runWith(s: DbSession[I]) = apply(s)

  /** Run the action in a transaction. */
   def runInTransaction(s: DbSession[I]) = {
    val connection = s.dbConnection
    try{
      connection.setAutoCommit(false)
      connection.setTransactionIsolation(s.jdbcLevel)
      val result = g(s)
      connection.commit()
      result
    } catch {
      case e => connection.rollback() ; throw e
    } finally {
      connection.setAutoCommit(true)
      connection.close()
    }
  }

  def map[B](f: A => B): Action[B,I] = Action[B,I]{ s =>
    val a: A = g(s)
    val b: B = f(a)
    b
  }

  def flatMap[B, II <: I](f: A => Action[B,II]): Action[B,II] = Action[B,II]{ s: DbSession[II] =>
    val a: A = g(s)
    val aB: Action[B,II] = f(a)
    val b: B = aB(s)
    b
  }

}

/**
 * This trait defines some implicit conversions for inter-operating with
 * scalaz's type classes.
 */
// trait ActionInstances {
// 
//   /**
//    * Ensure that Action is seen as a Monad and a Functor when using scalaz.
//    * For example, this is necessary when using it with monad transformers.
//    */
//   implicit val apiActionInstance = new Monad[Action] with Functor[Action] {
//     override def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] = fa flatMap f
//     override def point[A](a: => A): Action[A] = Action(_ => a )
//   }
// }
// 
// /**
//  * Companion object that captures all the implicits defined above
//  */
// object Action extends ActionInstances
