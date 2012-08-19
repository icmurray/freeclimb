package freeclimb.models

import freeclimb.validation._

import scalaz._
import Scalaz._

/**
 * The User domain model.
 *
 * The only required field is a username.  Everything else is optional.
 *
 * @param username required field
 *
 * @param realName optional real name of the user.
 *
 * @param email address of the user
 *
 * @param confirmed is whether the email address has been confirmed.
 *        None iff email is None
 */
class User private (
    val username: String,
    val realName: Option[String],
    val email: Option[String],
    val confirmed: Option[Boolean]) {

  override val hashCode = {
    username.hashCode + 41 * (
    realName.hashCode + 41 * (
    email.hashCode + 41 * (
    confirmed.hashCode
    )))
  }

  override def equals(that: Any) = that match {
    case u: User => {
      u.asInstanceOf[User].username  == this.username         &&
      u.asInstanceOf[User].realName  == this.realName         &&
      u.asInstanceOf[User].email     == this.email            &&
      u.asInstanceOf[User].confirmed == this.confirmed
    }
    case _       => false
  }

  override val toString = "User(" + username +")"

}

object User {

  def apply(username: String,
            realName: Option[String],
            email: Option[String],
            confirmed: Option[Boolean]) = (

    validateUsername(username)          .enrichAs("username")  |@|
    validateRealName(realName)          .enrichAs("real_name") |@|
    validateEmail(email)                .enrichAs("email")     |@|
    validateConfirmed(email, confirmed) .enrichAs("confirmed")

  ) { case _ => new User(username, realName, email, confirmed) }

  /**
   * Validation functions for each field of User
   */

  private val usernameChars = lowerAlpha ++ upperAlpha ++ numerAlpha
  private def validateUsername(implicit username: String) = {
    nonEmpty                    .liftFailNel <*
    maxLength(20)               .liftFailNel <*
    onlyContains(usernameChars) .liftFailNel
  }

  private def validateRealName(name: Option[String]) = name match {
    case None => name.success
    case Some(name) => {
      nonEmpty(name)      .liftFailNel <*
      maxLength(50)(name) .liftFailNel
    }
  }

  private def validateEmail(implicit email: Option[String]): Validation[String, Option[String]] = email.success

  private def validateConfirmed(email: Option[String], confirmed: Option[Boolean]) = (email, confirmed) match {
    case (None, None)       => None.success
    case (Some(e), Some(c)) => Some(c).success
    case _                  => "Confirmation failed.".fail
  }

}
