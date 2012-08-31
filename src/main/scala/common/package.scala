package freeclimb

import scalaz._
import Scalaz._

package object common {

  // I don't really like the name "\/".
  type Disjunction[+A,+B] = \/[A,B]
  type DisjunctionT[F[+_], +A, +B] = EitherT[F,A,B]

}
