package freeclimb

import scalaz._
import Scalaz._

import freeclimb.models._

package object api {
  
  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type ResultOrCA[T] = Validation[ConcurrentAccess[T], Revisioned[T]]
  type SuccessOrCA[T] = Validation[ConcurrentAccess[T], Unit]

}
