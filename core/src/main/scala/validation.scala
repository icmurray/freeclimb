package org.freeclimbers.core

import scalaz._

trait ValidationPackage {
  type Validated[T] = Validation[DomainError, T]
  type DomainError = List[String]
}
