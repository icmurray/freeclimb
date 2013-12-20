package org.freeclimbers
package core

import java.util.UUID

import scala.language.higherKinds
import scala.concurrent.{Future, future}

import scalaz._
import Scalaz._

/*****************************************************************************
 * Models
 *****************************************************************************/

case class ClimbId(uuid: UUID) extends AnyVal
object ClimbId {
  def createRandom() = ClimbId(UUID.randomUUID())
}

case class Climb(
    id: ClimbId,
    cragId: CragId,
    name: String,
    description: String)


/*****************************************************************************
 * Events
 *****************************************************************************/

sealed trait ClimbEvent
case class ClimbCreated(
    id: ClimbId,
    cragId: CragId,
    name: String,
    description: String) extends ClimbEvent

case class ClimbDeDuplicated(
    kept: Keep[ClimbId],
    removed: Remove[ClimbId]) extends ClimbEvent


/*****************************************************************************
 * Service Module
 *****************************************************************************/

/**
 * The Climb service interface definitions
 */
trait ClimbsModule[M[+_]] {
  implicit def M: Monad[M]

  val climbs: ClimbService

  trait ClimbService {

    // commands
    def create(name: String, description: String, crag: CragId): M[Validated[ClimbId]]
    def deDuplicate(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): M[Validated[ClimbId]]

    // queries
    // Note - query results are only *eventually* consistent with issued commands.
    def withId(id: ClimbId): M[Option[Climb]]
    def resolvesTo(id: ClimbId): M[Option[Climb]]
    def like(name: String): M[Seq[Climb]]
  }

}

/**
 * Eventsourced-based implementation of the climb service.
 */
trait EventsourcedClimbsModule extends ClimbsModule[Future] {
    this: ActorSystemModule =>

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  val climbs = new Impl()

  class Impl extends ClimbService {

    /*************************************************************************
     * Constructor body
     ************************************************************************/

    private[this] val readModel = {
      actorSystem.actorOf(Props(new ReadModel()), name = "climb-read-model")
    }

    private[this] val singleWriter = {
      actorSystem.actorOf(Props(new SingleWriter()), name = "climb-single-writer")
    }

    private[this] implicit val timeout = Timeout(5.seconds)
    private[this] val sharedProcessorId = "climb-service"


    /*************************************************************************
     * Service implementation
     ************************************************************************/

    def create(name: String, description: String, crag: CragId): Future[Validated[ClimbId]] = {
      val cmd = CreateCmd(crag, name, description)
      for {
        id    <- (singleWriter ? cmd).mapTo[ClimbId]
      } yield id.success
    }

    def deDuplicate(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): Future[Validated[ClimbId]] = {

      val cmd = DeDupeCmd(toKeep, toRemove)
      (singleWriter ? cmd).mapTo[Validated[ClimbId]]

    }

    def withId(id: ClimbId): Future[Option[Climb]] = {
      (readModel ? ClimbWithQ(id)).mapTo[Option[Climb]]
    }

    def resolvesTo(id: ClimbId): Future[Option[Climb]] = {
      (readModel ? ResolvesToQ(id)).mapTo[Option[Climb]]
    }

    def like(name: String): Future[Seq[Climb]] = ???

    /***************************************************************************
     * Private members
     **************************************************************************/

    /**
     * Queries
     */
    private sealed trait ClimbQ
    private case class ClimbWithQ(id: ClimbId) extends ClimbQ
    private case class ResolvesToQ(id: ClimbId) extends ClimbQ

    /**
     * Commands
     */
    private sealed trait Cmd
    private case class CreateCmd(cragId: CragId, name: String, description: String) extends Cmd
    private case class DeDupeCmd(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) extends Cmd


    /***************************************************************************
     * Private classes
     **************************************************************************/

    /**
     * Maintains an in-memory projection of the system's basic Climb information.
     *
     * It is an EventsourcedProcessor that is backed by the same journal as the
     * SingleWriter.  But it does *not* write the journal.  On start-up the
     * journal will be used to populate this actor with events, independantly of
     * the SingleWriter.  Meaning the two can fail independantly of one another.
     *
     * To query the state held by this Actor, send it `ClimbQ` messages.
     */
    private class ReadModel extends EventsourcedProcessor {

      private[this] var climbsImage = ClimbsImage()

      override def processorId = sharedProcessorId

      /**
       * Receiving replayed events.
       */
      val receiveReplay: Receive = {
        case e: ClimbEvent => updateState(e)
      }

      /**
       * Receiving query commands.
       */
      val receiveCommand: Receive = {
        case ClimbWithQ(id) =>
          sender ! climbsImage.byId.get(id)

        case ResolvesToQ(id) =>
          sender ! resolve(id)

        case p@Persistent(e: ClimbEvent,_) =>
          updateState(e)
          p.confirm()

        case e => println(s"Unhandled: ${e}")
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: ClimbEvent) = e match {
        case ClimbCreated(id, cragId, name, desc) =>
          climbsImage = climbsImage.addClimb((Climb(id, cragId, name, desc)))

        case ClimbDeDuplicated(kept, removed) =>
          climbsImage = climbsImage.deDupeClimb(kept, removed)
      }

      /**
       * Resolve a potential redirect.
       *
       * Follows chained re-directs until it encounters a re-direct to itself.
       * No attempt is made to break out of infinite loops as it assumed this is
       * taken care of by not allowing them to be constructed in the first place.
       */
      @annotation.tailrec
      private[this] def resolve(id: ClimbId): Option[Climb] = {
        climbsImage.redirects.get(id) match {
          case None                            => None
          case r@Some(climb) if climb.id == id => r
          case Some(climb)                     => resolve(climb.id)
        }
      }

      /**
       * The projection class itself.
       */
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

    /**
     * The single writer actor which executes write commands.
     */
    private class SingleWriter extends EventsourcedProcessor {

      private val channel = context.actorOf(Channel.props(), name="climb-events-channel")
      private var state = ClimbData()

      override def processorId = sharedProcessorId

      /**
       * Replayed events
       */
      val receiveReplay: Receive = {
        case evt: ClimbEvent =>
          updateState(evt)
      }

      /**
       * Commands
       */
      val receiveCommand: Receive = {
        case cmd: CreateCmd =>
          handleCreation(cmd)
        case cmd: DeDupeCmd =>
          handleDeDuplication(cmd)
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: ClimbEvent) = {
        e match {
          case e: ClimbCreated =>
            state = state.addClimb(e.id)

          case ClimbDeDuplicated(_, removed) =>
            state = state.removeClimb(removed.v)
        }
        notify(e)
      }

      private[this] def notify(e: ClimbEvent): Unit = {
        channel ! Deliver(Persistent.create(e), readModel)
      }

      private[this] def handleCreation(cmd: CreateCmd) = {
        val id = ClimbId.createRandom()
        val event = ClimbCreated(id, cmd.cragId, cmd.name, cmd.description)
        persist(event) { e =>
          updateState(e)
          sender ! id
        }
      }

      private[this] def handleDeDuplication(cmd: DeDupeCmd) = {

        val toKeep = cmd.toKeep.v
        val toRemove = cmd.toRemove.v

        if (toKeep == toRemove) {
          sender ! List("Cannot merge a climb with itself").failure
        } else if (! state.concreteIds.contains(toKeep) ||
                   ! state.concreteIds.contains(toRemove)) {
          sender ! List("Could not find both climb ids").failure
        } else {
          val event = ClimbDeDuplicated(cmd.toKeep, cmd.toRemove)
          persist(event) { e =>
            updateState(e)
            sender ! cmd.toKeep.v.success
          }
        }
      }


      /*************************************************************************
       * Private classes
       *************************************************************************/

      /**
       * The state required to ensure data integrity.
       */
      private[this] case class ClimbData(concreteIds: Set[ClimbId] = Set()) {
        def addClimb(id: ClimbId) = copy(concreteIds + id)
        def removeClimb(id: ClimbId) = copy(concreteIds - id)
      }
    }
  }
}
