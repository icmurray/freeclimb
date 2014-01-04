package org.freeclimbers.core

import scala.concurrent.Future

import scala.language.higherKinds

import scalaz._
import Scalaz._
import scalaz.contrib._

trait ValidationPackage {
  type Validated[T] = \/[DomainError, T]
  type ValidatedT[M[+_], T] = EitherT[M, DomainError, T]
  type DomainError = List[String]
}

trait ValidatedResults[M[+_]] {
  type Result[T] = ValidatedT[M, T]
  implicit def M: Monad[M]

  def Result[T](t: Validated[T]): Result[T] = EitherT(M.pure(t))
  def Result[T](tM: M[Validated[T]]): Result[T] = EitherT(tM)
}
