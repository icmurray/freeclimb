package freeclimb

import scalaz._
import Scalaz._

import freeclimb.models._

package object api {
  
  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type ResultOrCA[T] = SessionReader[Validation[ConcurrentAccess[T], Revisioned[T]]]
  type SuccessOrCA[T] = SessionReader[Validation[ConcurrentAccess[T], Unit]]

}
