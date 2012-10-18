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
  type ApiAction[+A, -I <: IsolationLevel] = ActionT[PossibleActionFailure,A,I,List[ActionEvent]]
  type ApiUpdateAction[+A] = ActionT[PossibleActionFailure,A,TransactionRepeatableRead,List[ActionEvent]]
  type ApiReadAction[+A] = ActionT[PossibleActionFailure,A,TransactionReadCommitted,List[ActionEvent]]
  type PossibleActionFailure[+A] = \/[ActionFailure, A]

  object ApiAction {
    def apply[A, I <: IsolationLevel](a: DbSession[I] => \/[ActionFailure, (List[ActionEvent], A)]) = ActionT[PossibleActionFailure,A,I,List[ActionEvent]](a)
  }

  object ApiUpdateAction {
    def apply[A](a: DbSession[TransactionRepeatableRead] => \/[ActionFailure, (List[ActionEvent], A)]) = ActionT[PossibleActionFailure,A,TransactionRepeatableRead, List[ActionEvent]](a)
  }

  object ApiReadAction {
    def apply[A](a: DbSession[TransactionReadCommitted] => \/[ActionFailure, A]) =
      ActionT[PossibleActionFailure,A,TransactionReadCommitted,List[ActionEvent]] { s => a(s) fold (
        error => error.left,
        success => (Nil, success).right
      )
      }

    def pure[A](v: => \/[ActionFailure, A]) = ApiReadAction(s => v)
  }
}
