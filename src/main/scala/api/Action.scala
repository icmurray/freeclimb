package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._
import freeclimb.sql.IsolationLevel

case class ActionT[M[+_], +A, -I <: IsolationLevel](g: DbSession[I] => M[A]) {

  def apply(s: DbSession[I]) = g(s)

  /** Synonym for apply() */
  def runWith(s: DbSession[I]) = apply(s)

  /** Run the action in a transaction. */
   def runInTransaction(s: DbSession[I])(implicit F: Failable[M[_]]) = {
    val connection = s.dbConnection
    try{
      connection.setAutoCommit(false)
      connection.setTransactionIsolation(s.jdbcLevel)
      val result = g(s)
      if (F.isFailure(result)) {
        connection.rollback()
      } else {
        connection.commit()
      }
      result
    } catch {
      case e => connection.rollback() ; throw e
    } finally {
      connection.setAutoCommit(true)
      connection.close()
    }
  }

  def map[B](f: A => B)(implicit F: Functor[M]): ActionT[M,B,I] = ActionT[M,B,I]{ s =>
    F.map(g(s))(f)
  }

  def flatMap[B, II <: I](f: A => ActionT[M,B,II])(implicit M: Bind[M]): ActionT[M,B,II] = ActionT[M,B,II]{ s =>
    M.bind(g(s))(f(_)(s))
  }

}

trait Failable[T] {
  def isFailure(t: T): Boolean
}

object Failable {
  implicit def disjunctionAsFailable: Failable[\/[_,_]] = new Failable[\/[_,_]] {
    override def isFailure(d: \/[_,_]) = d.isLeft
  }
  implicit val possibleActionFailureAsFailable: Failable[PossibleActionFailure[_]] = new Failable[PossibleActionFailure[_]] {
    override def isFailure(d: PossibleActionFailure[_]) = d.isLeft
  }
}

/**
 * This trait defines some implicit conversions for inter-operating with
 * scalaz's type classes.
 */
trait ActionTInstances {

  /**
   * Ensure that ActionT is seen as a Monad and a Functor when using scalaz.
   * For example, this is necessary when using it with monad transformers.
   */
  implicit def actionInstance[M[+_], I <: IsolationLevel](implicit M: Monad[M]) = new Monad[({type l[a] = ActionT[M, a, I]})#l] with Functor[({type l[a] = ActionT[M, a, I]})#l] {
    override def bind[A, B](fa: ActionT[M,A,I])(f: A => ActionT[M,B,I]): ActionT[M,B,I] = fa flatMap f
    override def point[A](a: => A): ActionT[M,A,I] = ActionT[M,A,I] { _ => M.point(a) }
  }
}

/**
 * Companion object that captures all the implicits defined above
 */
object ActionT extends ActionTInstances
