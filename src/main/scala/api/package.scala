package freeclimb

import scalaz._
import Scalaz._

import freeclimb.common._
import freeclimb.models._

package object api {
  
  /**
   * Some type synonyms to help tidy the function signatures.
   */
  type ActionResult[T] = DisjunctionT[ApiAction, ConcurrentAccess, Revisioned[T]]
  type ActionSuccess[T] = DisjunctionT[ApiAction, ConcurrentAccess, Unit]

}
