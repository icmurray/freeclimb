package freeclimb.api

import freeclimb.models._

/**
 * A disjoint union of classes representing possible concurrent access errors.
 */
sealed trait ConcurrentAccess
case class ConcurrentUpdate extends ConcurrentAccess
case class ConcurrentDelete extends ConcurrentAccess
