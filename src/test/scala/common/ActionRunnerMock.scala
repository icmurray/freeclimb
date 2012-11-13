package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.sql.IsolationLevel

class ActionRunnerMock extends ActionRunner {

  def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]]
        (action: ActionT[M,A,I,W])
        (implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A] = {
    val session = new DbSession[I] { val dbConnection = null }
    val result = action(session)
    result map { _._2 }
  }
}
