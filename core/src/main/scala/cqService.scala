package org.freeclimbers.core

import scala.language.higherKinds

import scalaz._
import Scalaz._

/**
 * A service that splits commands and queries.
 *
 * Command's perform validation, and hence may fail.  Queries don't.
 */
trait CQService[M[+_]] {
  type CResult[T] = ValidatedT[M, T]
  type QResult[T] = M[T]
  implicit def M: Monad[M]

  def CResult[T](t: Validated[T]): CResult[T] = EitherT(M.pure(t))
  def CResult[T](tM: M[Validated[T]]): CResult[T] = EitherT(tM)
}
