package freeclimb

import scalaz._
import Scalaz._

import _root_.freeclimb.common._
import _root_.freeclimb.models._

package object api {
  
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
  implicit def apiAction2EitherT[A](a: ApiAction[Disjunction[ConcurrentAccess[A], Revisioned[A]]]) = EitherT(a)
}
