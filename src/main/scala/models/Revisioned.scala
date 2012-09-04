package freeclimb.models

/**
 * A Revisioned[T] is a particular revision of a domain model, T.
 *
 * @param revision is a strictly-increasing integer.  Gaps in the sequence are
 *        fine.
 *
 * @param model is the underlying model value at the given revision.
 */
case class Revisioned[+T] (
  val revision: Long,
  val model: T
)
