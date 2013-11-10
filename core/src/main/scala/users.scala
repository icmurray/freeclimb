package org.freeclimbers.core

import java.util.UUID

import scala.collection.{mutable => m}
import scala.concurrent.{Future, future}
import scala.language.higherKinds

import org.mindrot.jbcrypt.BCrypt

import scalaz._
import Scalaz._

case class UserId(uuid: UUID) extends AnyVal
object UserId {
  def createRandom() = UserId(UUID.randomUUID)
}

case class UserToken(uuid: UUID) extends AnyVal
object UserToken {
  def generate() = UserToken(UUID.randomUUID())
}

case class Email(s: String) extends AnyVal
case class PlainText(s: String) extends AnyVal
case class Digest(s: String) extends AnyVal

case class User(
    id: UserId,
    email: Email,
    firstName: String,
    lastName: String,
    password: Digest)

sealed trait UserEvent
case class UserRegistered(
    id: UserId,
    email: Email,
    firstName: String,
    lastName: String,
    password: Digest) extends UserEvent {

  def createUser = User(id, email, firstName, lastName, password)

}

case class UserLoggedIn(id: UserId, token: UserToken) extends UserEvent
case class UserLoggedOut(id: UserId, token: UserToken) extends UserEvent

object Password {

  def hash(password: PlainText): Digest = {
    Digest(BCrypt.hashpw(password.s, BCrypt.gensalt()))
  }

  def check(candidate: PlainText, digest: Digest): Boolean = {
    BCrypt.checkpw(candidate.s, digest.s)
  }

}

object UserRegistered {
  def apply(email: Email,
            firstName: String,
            lastName: String,
            pass: PlainText): Validated[UserRegistered] = {

    (
      UserId.createRandom().success[DomainError] |@|
      validateEmail(email)                       |@|
      validateFirstName(firstName)               |@|
      validateLastName(lastName)                 |@|
      Password.hash(pass).success
    )(UserRegistered.apply _)
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

}

trait UsersModule[M[+_]] {

  implicit def M: Monad[M]

  val users: UserService

  trait UserService {

    def register(email: Email,
                 firstName: String, lastName: String,
                 pass: PlainText): M[Validated[User]]

    def login(email: Email,
              password: PlainText): M[Option[UserToken]]

    def logout(email: Email, token: UserToken): M[Unit]

    def authenticate(email: Email, password: PlainText): M[Option[User]]
    def authenticate(email: Email, token: UserToken): M[Option[User]]
  }

}

trait ActorUsersModule extends UsersModule[Future] {
  this: ActorSystemModule =>

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  val users = new Impl()

  class Impl extends UserService {

    private[this] val processor = {
      actorSystem.actorOf(Props(new Processor()), name = "user-processor")
    }

    private[this] implicit val timeout = Timeout(5.seconds)

    def register(email: Email, firstName: String, lastName: String, pass: PlainText) = {
      (processor ? RegisterCmd(email, firstName, lastName, pass)).mapTo[Validated[User]]
    }

    def login(email: Email,
              password: PlainText): Future[Option[UserToken]] = {
      (processor ? LoginCmd(email, password)).mapTo[Option[UserToken]]
    }

    def logout(email: Email, token: UserToken): Future[Unit] = {
      (processor ? LogoutCmd(email, token)).mapTo[Unit]
    }

    def authenticate(email: Email, password: PlainText): Future[Option[User]] = {
      (processor ? PasswordAuth(email, password)).mapTo[Option[User]]
    }

    def authenticate(email: Email, token: UserToken): Future[Option[User]] = {
      (processor ? TokenAuth(email, token)).mapTo[Option[User]]
    }
  }

  /**
   * The commands that control the Processor
   */
  private sealed trait Cmd
  private case class RegisterCmd(
      email: Email,
      firstName: String,
      lastName: String,
      pass: PlainText) extends Cmd {
    def validate = UserRegistered(email, firstName, lastName, pass)
  }

  private case class PasswordAuth(email: Email, pass: PlainText) extends Cmd
  private case class TokenAuth(email: Email, token: UserToken) extends Cmd
  private case class LoginCmd(email: Email, pass: PlainText) extends Cmd
  private case class LogoutCmd(email: Email, token: UserToken) extends Cmd

  private class Processor extends EventsourcedProcessor {

    private[this] var usersImage = UsersImage()

    private[this] def updateState(e: UserEvent) = e match {
      case e: UserRegistered =>
        usersImage = usersImage.addUser(e.createUser)
      case UserLoggedIn(id, token) =>
        usersImage = usersImage.login(id, token)
      case UserLoggedOut(id, token) =>
        usersImage = usersImage.logout(id, token)
    }

    /**
     * Handling *re-played* events.
     */
    val receiveReplay: Receive = {
      case evt: UserEvent =>
        updateState(evt)

      case SnapshotOffer(_, snapshot: UsersImage) =>
        usersImage = snapshot
    }

    /**
     * Handling external commands.
     */
    val receiveCommand: Receive = {
      case cmd: RegisterCmd =>
        handleRegister(cmd)
      case cmd: PasswordAuth =>
        handlePasswordAuth(cmd)
      case cmd: LoginCmd =>
        handleLogin(cmd)
      case cmd: TokenAuth =>
        handleTokenAuth(cmd)
      case cmd: LogoutCmd =>
        handleLogout(cmd)
    }

    private[this] def handleLogin(cmd: LoginCmd) = {
      authenticate(cmd.email, cmd.pass) match {
        case None       => sender ! None
        case Some(user) => {
          val token = UserToken.generate()
          val event = UserLoggedIn(user.id, token)
          persist(event) { e =>
            updateState(e)
            sender ! Some(token)
          }
        }
      }
    }

    private[this] def handleLogout(cmd: LogoutCmd) = {
      usersImage.tokens.get(cmd.token) match {
        case None       => sender ! {}
        case Some(user) => {
          val event = UserLoggedOut(user.id, cmd.token)
          persist(event) { e =>
            updateState(e)
            sender ! {}
          }
        }
      }
    }

    private[this] def authenticate(email: Email, pass: PlainText) = {
      for {
        user <- usersImage.byEmail.get(email)
        if Password.check(pass, user.password)
      } yield user
    }

    private[this] def authenticate(email: Email, token: UserToken) = {
      for {
        user <- usersImage.tokens.get(token)
        if user.email == email
      } yield user
    }

    private[this] def handlePasswordAuth(cmd: PasswordAuth) = {
      sender ! authenticate(cmd.email, cmd.pass)
    }

    private[this] def handleTokenAuth(cmd: TokenAuth) = {
      sender ! authenticate(cmd.email, cmd.token)
    }

    private[this] def handleRegister(cmd: RegisterCmd) = {
      val eventV = cmd.validate
      eventV.fold(
        invalid => sender ! eventV,
        event   => {
          val user = event.createUser
          usersImage.byEmail.get(user.email) match {
            case Some(_) =>
              sender ! List("User already exists").failure

            case None =>
              persist(event) { e =>
                updateState(e)
                sender ! user.success
              }
          }
        }
      )
    }

    private case class UsersImage(
        byId: Map[UserId, User] = Map(),
        byEmail: Map[Email, User] = Map(),
        tokens: Map[UserToken, User] = Map()) {

      def addUser(user: User): UsersImage = {
        UsersImage(
          byId + (user.id -> user),
          byEmail + (user.email -> user),
          tokens)
      }

      def login(id: UserId, token: UserToken): UsersImage = {
        val user = byId(id)
        UsersImage(
          byId, byEmail,
          tokens + (token -> user))
      }

      def logout(id: UserId, token: UserToken): UsersImage = {
        val user = byId(id)
        UsersImage(
          byId, byEmail,
          tokens - token)
      }

    }

  }

}

/**
 * This is **FULL** of race conditions.
 */
//trait InMemoryUsersModule[M[+_]] extends UsersModule[M] {
//
//  val users = new Impl()
//
//  class Impl extends UserService {
//
//    private[this] val users: m.Map[UserId, User] = {
//      new m.HashMap[UserId, User] with m.SynchronizedMap[UserId, User]
//    }
//
//    def register(email: Email, firstName: String, lastName: String, pass: PlainText) = {
//      // TODO: flesh out validation
//      M.pure {
//        users.values.find(_.email == email) match {
//          case None =>
//            val userV = User(email, firstName, lastName, pass)
//            userV.foreach { user =>
//              users += (user.id -> user)
//            }
//            userV
//          case Some(_) =>
//            List("User already exists").failure
//        }
//      }
//    }
//
//    def login(email: Email,
//              password: PlainText): M[Validated[UserToken]] = ???
//
//  }
//
//}

