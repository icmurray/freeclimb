package freeclimb.api

import scalaz._
import Scalaz._

import freeclimb.models._

/**
 * An ApiAction is an action that when handed an ApiSession, returns a value
 * of type `A`.
 *
 * It defines map and flatMap; and hence can be used in for expressions to
 * compose ApiActions together before running the composed ApiAction.
 *
 * It's really the Reader monad with the first type-parameter fixed to
 * `ApiSession`.
 */
case class ApiAction[+A](g: ApiSession => A) {

  /** Run the action by passing it an ApiSession */
  def apply(s: ApiSession) = g(s)

  /** Synonym for apply() */
  def runWith(s: ApiSession) = apply(s)

  def map[B](f: A => B): ApiAction[B] = {
    ApiAction(s => f(g(s)))
  }

  def flatMap[B](f: A => ApiAction[B]): ApiAction[B] = {
    ApiAction(s => f(g(s))(s))
  }

}

/**
 * This trait defines some implicit conversions for inter-operating with
 * scalaz's type classes.
 */
trait ApiActionInstances {

  /**
   * Ensure that ApiAction is seen as a Monad and a Functor when using scalaz.
   * For example, this is necessary when using it with monad transformers.
   */
  implicit val apiActionInstance = new Monad[ApiAction] with Functor[ApiAction] {
    override def bind[A, B](fa: ApiAction[A])(f: A => ApiAction[B]): ApiAction[B] = fa flatMap f
    override def point[A](a: => A): ApiAction[A] = ApiAction(_ => a )
  }
}

/**
 * Companion object that captures all the implicits defined above
 */
object ApiAction extends ApiActionInstances
