package freeclimb.api

import freeclimb.models._

/**
 * A disjoint union of classes representing possible concurrent access errors.
 */
sealed trait ConcurrentAccess[T]

/**
 * Upon a concurrent update, it's possible to retrieve the latest revision.
 */
case class ConcurrentUpdate[T](val revision: Revisioned[T]) extends ConcurrentAccess[T]

/**
 * Upoon a concurrent deletion, there's no latest data to represent
 */
case class ConcurrentDelete[T]() extends ConcurrentAccess[T]
