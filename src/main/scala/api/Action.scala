package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._
import freeclimb.sql.IsolationLevel

case class Action[M[+_], +A, -I <: IsolationLevel](g: DbSession[I] => M[A]) {

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

  def map[B](f: A => B)(implicit F: Functor[M]): Action[M,B,I] = Action[M,B,I]{ s =>
    F.map(g(s))(f)
  }

  def flatMap[B, II <: I](f: A => Action[M,B,II])(implicit M: Bind[M]): Action[M,B,II] = Action[M,B,II]{ s =>
    M.bind(g(s))(f(_)(s))
  }

}

/**
 * This trait defines some implicit conversions for inter-operating with
 * scalaz's type classes.
 */
trait ActionInstances {

  /**
   * Ensure that Action is seen as a Monad and a Functor when using scalaz.
   * For example, this is necessary when using it with monad transformers.
   */
  implicit def actionInstance[M[+_], I <: IsolationLevel](implicit M: Monad[M]) = new Monad[({type l[a] = Action[M, a, I]})#l] with Functor[({type l[a] = Action[M, a, I]})#l] {
    override def bind[A, B](fa: Action[M,A,I])(f: A => Action[M,B,I]): Action[M,B,I] = fa flatMap f
    override def point[A](a: => A): Action[M,A,I] = Action[M,A,I] { _ => M.point(a) }
  }
}

/**
 * Companion object that captures all the implicits defined above
 */
object Action extends ActionInstances
