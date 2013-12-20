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

case class CragId(uuid: UUID) extends AnyVal
object CragId {
  def createRandom() = CragId(UUID.randomUUID())
}

case class Crag(
    id: CragId,
    name: String,
    description: String)


/*****************************************************************************
 * Events
 *****************************************************************************/

sealed trait CragEvent
case class CragCreated(
    id: CragId,
    name: String,
    description: String) extends CragEvent


/*****************************************************************************
 * Service Module
 *****************************************************************************/

/**
 * The Crag service interface definitions
 */
trait CragsModule[M[+_]] {
  implicit def M: Monad[M]

  val crags: CragService

  trait CragService {

    // commands
    def create(name: String, description: String): M[Validated[CragId]]

    // queries
    // Note - query results are only *eventually* consistent with issued commands.
    def withId(id: CragId): M[Option[Crag]]
    def list(): M[Seq[Crag]]
  }

}

trait EventsourcedCragsModule extends CragsModule[Future] {
  this: ActorSystemModule =>

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  val crags = new Impl()

  class Impl extends CragService {

    /*************************************************************************
     * Constructor body
     ************************************************************************/

    private[this] val readModel = {
      actorSystem.actorOf(Props(new ReadModel()), name = "crag-read-model")
    }

    private[this] val singleWriter = {
      actorSystem.actorOf(Props(new SingleWriter()), name = "crag-single-writer")
    }

    private[this] implicit val timeout = Timeout(5.seconds)
    private[this] val sharedProcessorId = "crag-service"


    /*************************************************************************
     * Service implementation
     ************************************************************************/

    def create(name: String, description: String): Future[Validated[CragId]] = {
      val cmd = CreateCmd(name, description)
      (singleWriter ? cmd).mapTo[CragId].map(_.success)
    }

    def withId(id: CragId): Future[Option[Crag]] = {
      (readModel ? CragWithQ(id)).mapTo[Option[Crag]]
    }

    def list(): Future[Seq[Crag]] = {
      (readModel ? ListQ).mapTo[Seq[Crag]]
    }


    /***************************************************************************
     * Private members
     **************************************************************************/

    /**
     * Queries
     */
    private sealed trait CragQ
    private case class CragWithQ(id: CragId) extends CragQ
    private case object ListQ extends CragQ

    /**
     * Commands
     */
    private sealed trait Cmd
    private case class CreateCmd(name: String, description: String) extends Cmd


    /***************************************************************************
     * Private classes
     **************************************************************************/

    /**
     * Maintains an in-memory projection of the system's basic Crag information.
     *
     * It is an EventsourcedProcessor that is backed by the same journal as the
     * SingleWriter.  But it does *not* write the journal.  On start-up the
     * journal will be used to populate this actor with events, independantly of
     * the SingleWriter.  Meaning the two can fail independantly of one another.
     *
     * To query the state held by this Actor, send it `CragQ` messages.
     */
    private class ReadModel extends EventsourcedProcessor {

      private[this] var cragsImage = CragsImage()

      override def processorId = sharedProcessorId

      /**
       * Receiving replayed events.
       */
      val receiveReplay: Receive = {
        case e: CragEvent => updateState(e)
      }

      /**
       * Receiving query commands.
       */
      val receiveCommand: Receive = {
        case CragWithQ(id) =>
          sender ! cragsImage.byId.get(id)

        case ListQ =>
          sender ! cragsImage.byId.values.toSeq

        case p@Persistent(e: CragEvent,_) =>
          updateState(e)
          p.confirm()

        case e => println(s"Unhandled: ${e}")
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: CragEvent) = e match {
        case CragCreated(id, name, desc) =>
          cragsImage = cragsImage.addCrag((Crag(id, name, desc)))
      }

      /**
       * The projection class itself.
       */
      private[this] case class CragsImage(
          byId: Map[CragId, Crag] = Map()) {

        def addCrag(crag: Crag) = CragsImage(
          byId + (crag.id -> crag))

      }

    }

    /**
     * The single writer actor which executes write commands.
     */
    private class SingleWriter extends EventsourcedProcessor {

      private val channel = context.actorOf(Channel.props(), name="crag-events-channel")
      private var state = CragData()

      override def processorId = sharedProcessorId

      /**
       * Replayed events
       */
      val receiveReplay: Receive = {
        case evt: CragEvent =>
          updateState(evt)
      }

      /**
       * Commands
       */
      val receiveCommand: Receive = {
        case cmd: CreateCmd =>
          handleCreation(cmd)
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: CragEvent) = {
        e match {
          case e: CragCreated =>
            state = state.addCrag(e.id)
        }
        notify(e)
      }

      private[this] def notify(e: CragEvent): Unit = {
        channel ! Deliver(Persistent.create(e), readModel)
      }

      private[this] def handleCreation(cmd: CreateCmd) = {
        val id = CragId.createRandom()
        val event = CragCreated(id, cmd.name, cmd.description)
        persist(event) { e =>
          updateState(e)
          sender ! id
        }
      }


      /*************************************************************************
       * Private classes
       *************************************************************************/

      /**
       * The state required to ensure data integrity.
       */
      private[this] case class CragData(concreteIds: Set[CragId] = Set()) {
        def addCrag(id: CragId) = copy(concreteIds + id)
        def removeCrag(id: CragId) = copy(concreteIds - id)
      }
    }
  }
}
