package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._
import freeclimb.sql.IsolationLevel

case class ActionT[M[+_], +A, -I <: IsolationLevel, W <: List[ActionEvent]](g: DbSession[I] => M[(W, A)]) {

  def apply(s: DbSession[I]) = g(s)

  /** Synonym for apply() */
  def runWith(s: DbSession[I]) = apply(s)

  /** Run the action in a transaction. */
   def runInTransaction(s: DbSession[I])(implicit F: Failable[M[_]], M: Functor[M]) = {
    val connection = s.dbConnection
    try {
      connection.setAutoCommit(false)
      connection.setTransactionIsolation(s.jdbcLevel)
      val result = g(s)
      if (F.isFailure(result)) {
        connection.rollback()
      } else {
        connection.commit()
        val actions = M.map(result) { case (w,a) => w }
        println("Actions that occurred: ")
        M.map(actions) { l => l map println }
        println
      }

      M.map(result) { case (w,a) => a }
    } catch {
      case e => connection.rollback() ; throw e
    } finally {
      connection.setAutoCommit(true)
      connection.close()
    }
  }

  def map[B](f: A => B)(implicit F: Functor[M]): ActionT[M,B,I,W] = ActionT[M,B,I,W]{ s =>
    F.map(g(s)) {
      case (w, a) => (w, f(a))
    }
  }

  def flatMap[B, II <: I](f: A => ActionT[M,B,II,W])(implicit M: Bind[M], W: Semigroup[W]): ActionT[M,B,II,W] = ActionT[M,B,II,W]{ s =>
    M.bind(g(s)) {
      case (w1, a) => M.map(f(a)(s)) {
        case (w2, b) => (W.append(w1,w2), b)
      }
    }
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
  implicit def actionInstance[M[+_], I <: IsolationLevel, W <: List[ActionEvent]](implicit M: Monad[M], W: Monoid[W]) = new Monad[({type l[a] = ActionT[M, a, I, W]})#l] with Functor[({type l[a] = ActionT[M, a, I, W]})#l] {
    override def bind[A, B](fa: ActionT[M,A,I,W])(f: A => ActionT[M,B,I,W]): ActionT[M,B,I,W] = fa flatMap f
    override def point[A](a: => A): ActionT[M,A,I,W] = ActionT[M,A,I,W] { _ => M.point((W.zero, a)) }
  }
}

/**
 * Companion object that captures all the implicits defined above
 */
object ActionT extends ActionTInstances
