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

sealed trait RoutesDBEvent
case class CragCreated(
    id: CragId,
    name: String,
    description: String) extends RoutesDBEvent

case class ClimbCreated(
    cragId: CragId,
    climbId: ClimbId,
    name: String,
    description: String) extends RoutesDBEvent

case class ClimbsMerged(
    kept: Keep[ClimbId],
    removed: Remove[ClimbId]) extends RoutesDBEvent


/*****************************************************************************
 * Service Module
 *****************************************************************************/

/**
 * The Crag service interface definitions
 */
trait RoutesDatabaseModule[M[+_]] {
  implicit def M: Monad[M]

  val routesDB: RoutesDBService

  trait RoutesDBService {

    // commands
    def createCrag(name: String, description: String): M[Validated[CragId]]
    def createClimb(name: String, description: String, crag: CragId): M[Validated[ClimbId]]
    def mergeClimbs(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): M[Validated[ClimbId]]

    // queries
    // Note - query results are only *eventually* consistent with issued commands.
    def cragById(id: CragId): M[Option[Crag]]
    def climbById(id: ClimbId): M[Option[Climb]]
    def crags(): M[Seq[Crag]]
    def climbs(): M[Seq[Climb]]
    def climbsOf(crag: CragId): M[Seq[Climb]]
  }

}

trait EventsourcedRoutesDatabaseModule extends RoutesDatabaseModule[Future] {
  this: ActorSystemModule =>

  import akka.actor._
  import akka.pattern.ask
  import akka.persistence._
  import akka.util.Timeout
  import scala.concurrent.duration._

  val routesDB = new Impl()

  class Impl extends RoutesDBService {

    /*************************************************************************
     * Constructor body
     ************************************************************************/

    private[this] val readModel = {
      actorSystem.actorOf(Props(new RoutesDatabase()), name = "routes-db-read-model")
    }

    private[this] val singleWriter = {
      actorSystem.actorOf(Props(new SingleWriter()), name = "routes-db-single-writer")
    }

    private[this] implicit val timeout = Timeout(5.seconds)
    private[this] val sharedProcessorId = "routes-db-service"


    /*************************************************************************
     * Service implementation
     ************************************************************************/

    def createCrag(name: String, description: String): Future[Validated[CragId]] = {
      val cmd = CreateCragCmd(name, description)
      (singleWriter ? cmd).mapTo[CragId].map(_.success)
    }

    def createClimb(name: String, description: String, crag: CragId) = ???
    def mergeClimbs(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) = ???

    def cragById(id: CragId): Future[Option[Crag]] = {
      (readModel ? CragByIdQ(id)).mapTo[Option[Crag]]
    }

    def climbById(id: ClimbId) = ???
    def climbs() = ???
    def climbsOf(crag: CragId) = ???

    def crags(): Future[Seq[Crag]] = {
      (readModel ? ListCragsQ).mapTo[Seq[Crag]]
    }


    /***************************************************************************
     * Private members
     **************************************************************************/

    /**
     * Queries
     */
    private sealed trait CragQ
    private case class ClimbByIdQ(id: ClimbId) extends CragQ
    private case class CragByIdQ(id: CragId) extends CragQ
    private case class ListClimbsOfQ(id: CragId) extends CragQ
    private case object ListCragsQ extends CragQ
    private case object ListClimbsQ extends CragQ

    /**
     * Commands
     */
    private sealed trait Cmd

    private case class CreateClimbCmd(
        name: String,
        description: String,
        crag: CragId) extends Cmd

    private case class CreateCragCmd(
        name: String, 
        description: String) extends Cmd

    private case class MergeClimbsCmd(
        toKeep: Keep[ClimbId],
        toRemove: Remove[ClimbId]) extends Cmd


    /***************************************************************************
     * Private classes
     **************************************************************************/

    /**
     * Maintains an in-memory projection of the system's basic routes database.
     *
     * It is an EventsourcedProcessor that is backed by the same journal as the
     * SingleWriter.  But it does *not* write the journal.  On start-up the
     * journal will be used to populate this actor with events, independantly of
     * the SingleWriter.  Meaning the two can fail independantly of one another.
     *
     * To query the state held by this Actor, send it `CragQ` messages.
     */
    private class RoutesDatabase extends EventsourcedProcessor {

      private[this] var crags = CragsRepo()
      private[this] var climbs = ClimbsRepo()

      override def processorId = sharedProcessorId

      /**
       * Receiving replayed events.
       */
      val receiveReplay: Receive = {
        case e: RoutesDBEvent => updateState(e)
      }

      /**
       * Receiving query commands.
       */
      val receiveCommand: Receive = {
        case CragByIdQ(id) =>
          sender ! crags.byId.get(id)

        case ClimbByIdQ(id) =>
          sender ! climbs.byId.get(id)

        case ListCragsQ =>
          sender ! crags.byId.values.toSeq

        case ListClimbsQ =>
          sender ! climbs.byId.values.toSeq

        case ListClimbsOfQ(cragId) =>
          sender ! climbs.byCrag.get(cragId).getOrElse(Seq())

        case p@Persistent(e: RoutesDBEvent,_) =>
          updateState(e)
          p.confirm()

        case e => println(s"Unhandled: ${e}")
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: RoutesDBEvent) = e match {
        case CragCreated(id, name, desc) =>
          crags = crags.addCrag((Crag(id, name, desc)))
        case ClimbCreated(cragId, climbId, name, description) =>
          climbs = climbs.addClimb(Climb(climbId, cragId, name, description))
      }

      /**
       * The projection class itself.
       */
      private[this] case class CragsRepo(
          byId: Map[CragId, Crag] = Map()) {

        def addCrag(crag: Crag) = CragsRepo(
          byId + (crag.id -> crag))

      }

      private[this] case class ClimbsRepo(
          byId: Map[ClimbId, Climb] = Map(),
          byCrag: Map[CragId, Seq[Climb]] = Map(),
          redirects: Map[ClimbId, Climb] = Map()) {

        def addClimb(climb: Climb) = ClimbsRepo(
          byId + (climb.id -> climb),
          adjust(byCrag, climb.cragId) {
            case None         => Seq()
            case Some(climbs) => climb +: climbs
          },
          redirects + (climb.id -> climb))

        def deDupeClimb(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) = {
          val keep = redirects(toKeep.v)
          ClimbsRepo(
            byId - toRemove.v,
            byCrag,
            redirects + (toRemove.v -> keep))
        }   

        private[this] def adjust[A,B](m: Map[A,B], k: A)(f: Option[B] => B) = m.updated(k, f(m.get(k)))
      }



    }

    /**
     * The single writer actor which executes write commands.
     */
    private class SingleWriter extends EventsourcedProcessor {

      private val channel = context.actorOf(Channel.props(), name="routes-db-events-channel")
      private var state = CragData()

      override def processorId = sharedProcessorId

      /**
       * Replayed events
       */
      val receiveReplay: Receive = {
        case evt: RoutesDBEvent =>
          updateState(evt)
      }

      /**
       * Commands
       */
      val receiveCommand: Receive = {
        case cmd: CreateCragCmd =>
          handleCragCreation(cmd)
        case cmd: CreateClimbCmd =>
          handleClimbCreation(cmd)
        case cmd: MergeClimbsCmd =>
          handleMerge(cmd)
      }


      /*************************************************************************
       * Private members
       *************************************************************************/

      private[this] def updateState(e: RoutesDBEvent) = {
        e match {
          case e: CragCreated =>
            state = state.addCrag(e.id)
          case e: ClimbCreated =>
            state = state.addClimb(e.climbId)
        }
        notify(e)
      }

      private[this] def notify(e: RoutesDBEvent): Unit = {
        channel ! Deliver(Persistent.create(e), readModel)
      }

      private[this] def handleCragCreation(cmd: CreateCragCmd) = {
        val id = CragId.createRandom()
        val event = CragCreated(id, cmd.name, cmd.description)
        persist(event) { e =>
          updateState(e)
          sender ! id
        }
      }

      private[this] def handleClimbCreation(cmd: CreateClimbCmd) = {
        val id = ClimbId.createRandom()
        val event = ClimbCreated(cmd.crag, id, cmd.name, cmd.description)
        persist(event) { e =>
          updateState(e)
          sender ! id
        }
      }

      private[this] def handleMerge(cmd: MergeClimbsCmd) = {
        ???
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
        def addClimb(id: ClimbId) = ???
      }
    }
  }
}
