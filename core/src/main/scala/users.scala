package org.freeclimbers.core

import java.util.UUID

import scala.collection.{mutable => m}
import scala.concurrent.{Future, future}
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
    naiveEmailRx findFirstIn email.s.trim match {
      case Some(s) => Email(s).success
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
      user: User) extends Cmd

  private case class LoginCmd(
      email: Email,
      pass: PlainText) extends Cmd

  private class Processor extends EventsourcedProcessor {

    private case class UsersImage(
        byId: Map[UserId, User] = Map(),
        byEmail: Map[Email, User] = Map()) {

      def addUser(user: User): UsersImage = {
        UsersImage(
          byId + (user.id -> user),
          byEmail + (user.email -> user))
      }

    }

    private[this] var usersImage = UsersImage()

    private[this] def updateState(e: UserEvent) = e match {
      case UserRegistered(user) =>
        usersImage = usersImage.addUser(user)
    }

    val receiveReplay: Receive = {

      case evt: UserEvent =>
        updateState(evt)

      case SnapshotOffer(_, snapshot: UsersImage) =>
        usersImage = snapshot
    }

    val receiveCommand: Receive = {

      case RegisterCmd(user) =>
        usersImage.byEmail.get(user.email) match {

          case None =>
            persist(UserRegistered(user)) { event =>
              updateState(event)
              sender ! user.success
            }

          case Some(_) =>
            sender ! List("User already exists").failure
        }
    }

  }

  val actorSystem: ActorSystem
  implicit val ec = actorSystem.dispatcher
  val users = new Impl()

  class Impl extends UserService {

    private val processor = actorSystem.actorOf(Props(new Processor()), name = "user-processor")
    private implicit val timeout = Timeout(5.seconds)

    def register(email: Email, firstName: String, lastName: String, pass: PlainText) = {
      User(email, firstName, lastName, pass).fold(
        invalid => future { invalid.failure },
        user    => (processor ? RegisterCmd(user)).mapTo[Validated[User]]
      )
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

