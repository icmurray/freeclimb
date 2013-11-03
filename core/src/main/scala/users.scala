package org.freeclimbers.core

import java.util.UUID

import scala.collection.{mutable => m}
import scala.concurrent.Future
import scala.language.higherKinds

import org.mindrot.jbcrypt.BCrypt

import scalaz._
import Scalaz._

case class UserId(uuid: UUID) extends AnyVal

object UserId {
  def createRandom() = {
    UserId(UUID.randomUUID)
  }
}

case class Email(s: String) extends AnyVal
case class UserToken(uuid: UUID) extends AnyVal
case class PlainText(s: String) extends AnyVal
case class Digest(s: String) extends AnyVal

case class User(
    id: UserId,
    email: Email,
    firstName: String,
    lastName: String,
    password: Digest)

object User {
  def apply(email: Email,
            firstName: String,
            lastName: String,
            pass: PlainText): Validated[User] = {

    (
      UserId.createRandom().success[DomainError] |@|
      validateEmail(email)                       |@|
      validateFirstName(firstName)               |@|
      validateLastName(lastName)                 |@|
      hash(pass).success
    )(User.apply _)
  }

  private final def validateEmail(email: Email): Validated[Email] = email.success
  private final def validateFirstName(name: String): Validated[String] = name.success
  private final def validateLastName(name: String): Validated[String] = name.success

  protected def hash(password: PlainText): Digest = {
    Digest(BCrypt.hashpw(password.s, BCrypt.gensalt()))
  }

  protected def check(candidate: PlainText, digest: Digest): Boolean = {
    BCrypt.checkpw(candidate.s, digest.s)
  }

}

trait UsersModule[M[+_]] {

  implicit def M: Monad[M]

  val users: UserService

  trait UserService {

    def register(email: Email,
                 firstName: String, lastName: String,
                 pass: PlainText): M[Validated[User]]

    def login(email: Email,
              password: PlainText): M[Validated[UserToken]]

  }

}

/**
 * This is **FULL** of race conditions.
 */
trait InMemoryUsersModule[M[+_]] extends UsersModule[M] {

  val users = new Impl()

  class Impl extends UserService {

    private[this] val users: m.Map[UserId, User] = {
      new m.HashMap[UserId, User] with m.SynchronizedMap[UserId, User]
    }

    def register(email: Email, firstName: String, lastName: String, pass: PlainText) = {
      // TODO: flesh out validation
      M.pure {
        users.values.find(_.email == email) match {
          case None =>
            val userV = User(email, firstName, lastName, pass)
            userV.foreach { user =>
              users += (user.id -> user)
            }
            userV
          case Some(_) =>
            List("User already exists").failure
        }
      }
    }

    def login(email: Email,
              password: PlainText): M[Validated[UserToken]] = ???

  }

}

