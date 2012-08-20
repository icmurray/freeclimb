package freeclimb.api

import freeclimb.models.User

/**
 * The ApiSession trait.
 *
 * Each action in the api requires an ApiSession object to be passed into it.
 * This allows some sort of context to be passed in.  This includes the User
 * performing the action (so that authorization can be carried out), and will
 * include things like a database session.
 */
trait ApiSession {
  val user: Option[User]
}
