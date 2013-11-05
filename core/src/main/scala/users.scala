package org.freeclimbers.core

import java.util.UUID

import scala.collection.{mutable => m}
import scala.concurrent.Future
import scala.language.higherKinds

import org.mindrot.jbcrypt.BCrypt

import scalaz._
import Scalaz._

sealed trait UserEvent
case class UserRegistered(user: User) extends UserEvent
case class UserLoggedIn(user: User) extends UserEvent
case class UserLoggedOut(user: User) extends UserEvent

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

  private val naiveEmailRx = """^[^@]+@.+""".r
  private final def validateEmail(email: Email) = {
    naiveEmailRx findFirstIn email.s match {
      case Some(_) => email.success
      case None    => List("Invalid Email").failure
    }
  }

  private final def validateFirstName(name: String) = {
    name.trim match {
      case "" => List("First name must not be empty").failure
      case s  => s.success
    }
  }

  private final def validateLastName(name: String) = name.success

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

trait ActorUsersModule extends UsersModule[Future] {

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  private sealed trait Cmd

  private case class RegisterCmd(
      email: Email,
      firstName: String,
      lastName: String,
      pass: PlainText) extends Cmd

  private case class LoginCmd(
      email: Email,
      pass: PlainText) extends Cmd

  private class Processor extends EventsourcedProcessor {

    private[this] val usersById    = m.Map[UserId, User]()
    private[this] val usersByEmail = m.Map[Email, User]()

    private[this] def updateState(e: UserEvent) = ???

    val receiveReplay: Receive = {
      case evt: UserEvent => updateState(evt)
    }

    val receiveCommand: Receive = {

      case RegisterCmd(email, fN, lN, pass) =>
        usersByEmail.get(email) match {

          case None =>
            val userV = User(email, fN, lN, pass)
            userV.fold (
              { failure =>
                sender ! userV
              },

              { user =>
                persist(UserRegistered(user)) { event =>
                  updateState(event)
                  sender ! userV
                }
              }
            )

          case Some(_) =>
            sender ! List("User already exists").failure
        }
    }

  }

  val system: ActorSystem
  val users = new Impl()

  class Impl extends UserService {

    private val processor = system.actorOf(Props[Processor], "user-processor")
    private implicit val timeout = Timeout(5.seconds)

    def register(email: Email, firstName: String, lastName: String, pass: PlainText) = {
      val f = processor ? RegisterCmd(email, firstName, lastName, pass)
      f.mapTo[Validated[User]]
    }

    def login(email: Email,
              password: PlainText): Future[Validated[UserToken]] = ???

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

