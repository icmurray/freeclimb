package freeclimb

import scalaz._
import Scalaz._

import freeclimb.common._
import freeclimb.models._
import freeclimb.sql.IsolationLevel

package object api {
  
  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type Action[+A, -I <: IsolationLevel] = ActionT[Id,A,I]
  type ActionFailure[+A] = Disjunction[ConcurrentAccess, A]
  type ActionResult[+A, -I <: IsolationLevel] = ActionT[ActionFailure, Revisioned[A], I]

  //type ActionResult[T] = DisjunctionT[ApiAction, ConcurrentAccess, Revisioned[T]]
  //type ActionSuccess[T] = DisjunctionT[ApiAction, ConcurrentAccess, Unit]

  object Action {
    def apply[A, I <: IsolationLevel](g: DbSession[I] => Id[A]) = ActionT[Id,A,I](g)
  }

}
