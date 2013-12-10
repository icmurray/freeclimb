package org.freeclimbers
package core

import java.util.UUID

import scala.language.higherKinds
import scala.concurrent.{Future, future}

import scalaz._
import Scalaz._

case class ClimbId(uuid: UUID) extends AnyVal
object ClimbId {
  def createRandom() = ClimbId(UUID.randomUUID())
}

case class Climb(
    id: ClimbId,
    name: String)

case class Keep[T](v: T) extends AnyVal
case class Remove[T](v: T) extends AnyVal

sealed trait ClimbEvent
case class ClimbCreated(
    id: ClimbId,
    name: String) extends ClimbEvent

case class ClimbDeDuplicated(
    kept: Keep[ClimbId],
    removed: Remove[ClimbId]) extends ClimbEvent

trait ClimbService[M[+_]] extends ClimbServiceWrites[M] with ClimbServiceReads[M]

trait ClimbServiceWrites[M[+_]] {
  def create(name: String): M[Validated[Climb]]
  def deDuplicate(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): M[Validated[ClimbId]]
}

trait ClimbServiceReads[M[+_]] {
  def withId(id: ClimbId): M[Option[Climb]]
  def resolvesTo(id: ClimbId): M[Option[Climb]]
  def like(name: String): M[Seq[Climb]]
}

trait ClimbsModule[M[+_]] {
  implicit def M: Monad[M]

  val climbs: ClimbService[M]
}

trait ActorClimbsModule extends ClimbsModule[Future] {
    this: ActorSystemModule =>

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  //implicit def M = scalaFuture.futureInstance
  val climbs = new Impl()

  class Impl extends ClimbService[Future] {


    // TODO: how should these actors fail together?
    private[this] val queryState = {
      actorSystem.actorOf(Props(new QueryStateActor()), name = "climb-query-state")
    }

    private[this] val singleWriter = {
      actorSystem.actorOf(Props(new SingleWriter(queryState)), name = "climb-single-writer")
    }

    private[this] implicit val timeout = Timeout(5.seconds)

    def create(name: String): Future[Validated[Climb]] = {
      val cmd = CreateCmd(name)
      for {
        id    <- (singleWriter ? cmd).mapTo[ClimbId]
        climb <- (queryState ? ClimbWithQ(id)).mapTo[Option[Climb]]
      } yield climb.get.success
    }

    def deDuplicate(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): Future[Validated[ClimbId]] = {
      val keepF = withId(toKeep.v)
      val removeF = withId(toRemove.v)

      for {
        keep <- keepF
        remove <- removeF

        result <- if (keep.nonEmpty && remove.nonEmpty) {
          val cmd = DeDupeCmd(toKeep, toRemove)
          (singleWriter ? cmd).mapTo[ClimbId].map(_.success)
        } else {
          future { List("Couldn't find both climb ids").failure }
        }
      } yield result

    }

    def withId(id: ClimbId): Future[Option[Climb]] = {
      (queryState ? ClimbWithQ(id)).mapTo[Option[Climb]]
    }

    def resolvesTo(id: ClimbId): Future[Option[Climb]] = {
      (queryState ? ResolvesToQ(id)).mapTo[Option[Climb]]
    }

    def like(name: String): Future[Seq[Climb]] = ???
  }

  private sealed trait ClimbQ
  private case class ClimbWithQ(id: ClimbId) extends ClimbQ
  private case class ResolvesToQ(id: ClimbId) extends ClimbQ

  private class QueryStateActor extends Actor {

    private[this] var climbsImage = ClimbsImage()

    def receive = {

      case ClimbWithQ(id) =>
        sender ! climbsImage.byId.get(id)

      case ResolvesToQ(id) =>
        sender ! resolve(id)

      case ClimbCreated(id, name) =>
        climbsImage = climbsImage.addClimb(Climb(id, name))

      case ClimbDeDuplicated(kept, removed) =>
        climbsImage = climbsImage.deDupeClimb(kept, removed)
    }

    private[this] def resolve(id: ClimbId): Option[Climb] = climbsImage.redirects.get(id)

    private[this] case class ClimbsImage(
        byId: Map[ClimbId, Climb] = Map(),
        redirects: Map[ClimbId, Climb] = Map()) {

      def addClimb(climb: Climb) = ClimbsImage(
        byId + (climb.id -> climb),
        redirects + (climb.id -> climb))

      def deDupeClimb(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) = {
        val keep = redirects(toKeep.v)
        ClimbsImage(
          byId - toRemove.v,
          redirects + (toRemove.v -> keep))
      }   
    }

  }

  private sealed trait Cmd
  private case class CreateCmd(name: String) extends Cmd
  private case class DeDupeCmd(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) extends Cmd

  private class SingleWriter(queryStateRef: ActorRef) extends EventsourcedProcessor {

    private[this] def updateState(e: ClimbEvent) = e match {
      case e: ClimbCreated =>
      {}
      case e: ClimbDeDuplicated =>
      {}
    }

    val receiveReplay: Receive = {
      case evt: ClimbEvent =>
        updateState(evt)

      //case SnapshotOffer(_, snapshot: ClimbsImage) =>
      //  climbsImage = snapshot
    }

    val receiveCommand: Receive = {
      case cmd: CreateCmd =>
        handleCreation(cmd)
      case cmd: DeDupeCmd =>
        handleDeDuplication(cmd)
    }

    private[this] def handleCreation(cmd: CreateCmd) = {
      val id = ClimbId.createRandom()
      val event = ClimbCreated(id, cmd.name)
      persist(event) { e =>
        updateState(e)
        queryStateRef ! e
        sender ! id
      }
    }

    private[this] def handleDeDuplication(cmd: DeDupeCmd) = {
      val event = ClimbDeDuplicated(cmd.toKeep, cmd.toRemove)
      persist(event) { e =>
        updateState(e)
        queryStateRef ! e
        sender ! cmd.toKeep.v
      }
    }

  }

}
