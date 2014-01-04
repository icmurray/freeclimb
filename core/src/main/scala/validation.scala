package org.freeclimbers.core

import scala.language.higherKinds

import scalaz._
import Scalaz._

trait ValidationPackage {
  type Validated[T] = \/[DomainError, T]
  type ValidatedT[M[+_], T] = EitherT[M, DomainError, T]
  type DomainError = List[String]
}
