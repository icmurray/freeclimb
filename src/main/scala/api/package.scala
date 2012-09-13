package freeclimb

import scalaz._
import Scalaz._

import freeclimb.common._
import freeclimb.models._
import freeclimb.sql._

package object api {
  
  /**
   * Some type synonyms to help tidy the function signatures.
   */
  //type Action[+A, -I <: IsolationLevel] = ActionT[Id,A,I]
  //type ActionFailure[+A] = Disjunction[ActionFailure, A]
  //type ActionResult[+A, -I <: IsolationLevel] = ActionT[ActionFailure, Revisioned[A], I]
  type ApiAction[+A, -I <: IsolationLevel] = ActionT[PossibleActionFailure,A,I]
  type ApiUpdateAction[+A] = ActionT[PossibleActionFailure,A,TransactionRepeatableRead]
  type ApiReadAction[+A] = ActionT[PossibleActionFailure,A,TransactionReadCommitted]
  type PossibleActionFailure[+A] = \/[ActionFailure, A]

  object ApiAction {
    def apply[A, I <: IsolationLevel](a: DbSession[I] => \/[ActionFailure, A]) = ActionT[PossibleActionFailure,A,I](a)
  }

  object ApiUpdateAction {
    def apply[A](a: DbSession[TransactionRepeatableRead] => \/[ActionFailure, A]) = ActionT[PossibleActionFailure,A,TransactionRepeatableRead](a)
  }

  object ApiReadAction {
    def apply[A](a: DbSession[TransactionReadCommitted] => \/[ActionFailure, A]) = ActionT[PossibleActionFailure,A,TransactionReadCommitted](a)
  }
}
