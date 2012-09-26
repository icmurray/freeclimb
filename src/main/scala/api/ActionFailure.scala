package freeclimb.api

/*
 * Used to represent the failure to run an api action.  This encompasses a
 * a lot of things from an optimistic concurrency failure, to ...
 */

sealed trait ActionFailure

final case class EditConflict extends ActionFailure
final case class NotFound extends ActionFailure
final case class NotImplemented extends ActionFailure
