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
trait RoutesDatabaseModule[M[+_]] extends CQService[M] {
  implicit def M: Monad[M]

  val routesDB: RoutesDBService

  trait RoutesDBService {

    // commands
    def createCrag(name: String, description: String): CResult[CragId]
    def createClimb(name: String, description: String, crag: CragId): CResult[ClimbId]
    def mergeClimbs(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]): CResult[ClimbId]

    // queries
    // Note - query results are only *eventually* consistent with issued commands.
    def cragById(id: CragId): QResult[Option[Crag]]
    def climbById(id: ClimbId): QResult[Option[Climb]]
    def resolveClimb(id: ClimbId): QResult[Option[Climb]]
    def crags(): QResult[Seq[Crag]]
    def climbs(): QResult[Seq[Climb]]
    def climbsOf(crag: CragId): QResult[Seq[Climb]]
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

    def createCrag(name: String, description: String): CResult[CragId] = {
      for {
        cmd <- CreateCragCmd.validate(name, description)
        id  <- CResult((singleWriter ? cmd).mapTo[Validated[CragId]])
      } yield id
    }

    def createClimb(name: String, description: String, crag: CragId) = {
      for {
        cmd <- CreateClimbCmd.validate(name, description, crag)
        id  <- CResult((singleWriter ? cmd).mapTo[Validated[ClimbId]])
      } yield id
    }

    def mergeClimbs(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) = {
      val cmd = MergeClimbsCmd(toKeep, toRemove)
      CResult((singleWriter ? cmd).mapTo[Validated[ClimbId]])
    }

    def cragById(id: CragId) = {
      (readModel ? CragByIdQ(id)).mapTo[Option[Crag]]
    }

    def resolveClimb(id: ClimbId) = {
      (readModel ? ClimbResolvesToQ(id)).mapTo[Option[Climb]]
    }

    def climbById(id: ClimbId) = {
      (readModel ? ClimbByIdQ(id)).mapTo[Option[Climb]]
    }

    def climbs() = {
      (readModel ? ListClimbsQ).mapTo[Seq[Climb]]
    }

    def climbsOf(crag: CragId) = {
      (readModel ? ListClimbsOfQ(crag)).mapTo[Seq[Climb]]
    }

    def crags(): Future[Seq[Crag]] = {
      (readModel ? ListCragsQ).mapTo[Seq[Crag]]
    }


    /***************************************************************************
     * Private members
     **************************************************************************/

    /**
     * Queries
     */
    private sealed trait RoutesDBQ
    private case class ClimbByIdQ(id: ClimbId) extends RoutesDBQ
    private case class CragByIdQ(id: CragId) extends RoutesDBQ
    private case class ListClimbsOfQ(id: CragId) extends RoutesDBQ
    private case class ClimbResolvesToQ(id: ClimbId) extends RoutesDBQ
    private case object ListCragsQ extends RoutesDBQ
    private case object ListClimbsQ extends RoutesDBQ

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

    /**
     * Command's companion objects.  Mostly validation.
     */
    private object CreateClimbCmd {
      def validate(name: String,
                   description: String,
                   crag: CragId): CResult[CreateClimbCmd] = CResult(
        (
          validateName(name)  |@|
          description.success |@|
          crag.success
        )(CreateClimbCmd.apply _).disjunction
     )

      private[this] def validateName(name: String) = {
        name.trim match {
          case "" => List("Climb name cannot be blank").failure
          case s  => s.success
        }
      }
    }

    private object CreateCragCmd {
      def validate(name: String, description: String) = CResult(
        (
          validateName(name)  |@|
          description.success
        )(CreateCragCmd.apply _).disjunction
      )

      private[this] def validateName(name: String) = {
        name.trim match {
          case "" => List("Crag name cannot be blank").failure
          case s  => s.success
        }
      }
    }


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

        case ClimbResolvesToQ(id) =>
          sender ! resolveClimb(id)

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
        case ClimbsMerged(kept, removed) =>
          climbs = climbs.mergeClimbs(kept, removed)
      }

      /**
       * Resolve a potential redirect.
       *
       * Follows chained re-directs until it encounters a re-direct to itself.
       * No attempt is made to break out of infinite loops as it assumed this is
       * taken care of by not allowing them to be constructed in the first place.
       */
      @annotation.tailrec
      private[this] def resolveClimb(id: ClimbId): Option[Climb] = {
        climbs.redirects.get(id) match {
          case None                            => None
          case r@Some(climb) if climb.id == id => r
          case Some(climb)                     => resolveClimb(climb.id)
        }
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
            case None         => Seq(climb)
            case Some(climbs) => climb +: climbs
          },
          redirects + (climb.id -> climb))

        def mergeClimbs(toKeep: Keep[ClimbId], toRemove: Remove[ClimbId]) = {
          val keep = redirects(toKeep.v)
          ClimbsRepo(
            byId - toRemove.v,
            byCrag.mapValues(_.filter(_.id != toRemove.v)),
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
      private var state = State()

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
          case ClimbsMerged(_, Remove(id)) =>
            state = state.removeClimb(id)
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
          sender ! id.right
        }
      }

      private[this] def handleClimbCreation(cmd: CreateClimbCmd) = {
        state.cragIds.contains(cmd.crag) match {
          case false =>
            sender ! List("Specified crag does not exist").left
          case true  =>
            val id = ClimbId.createRandom()
            val event = ClimbCreated(cmd.crag, id, cmd.name, cmd.description)
            persist(event) { e =>
              updateState(e)
              sender ! id.right
            }
        }
      }

      private[this] def handleMerge(cmd: MergeClimbsCmd) = {

        val toKeep = cmd.toKeep.v
        val toRemove = cmd.toRemove.v

        if (toKeep == toRemove) {
          sender ! List("Cannot merge a climb with itself").left
        } else if (! state.concreteClimbIds.contains(toKeep) ||
                   ! state.concreteClimbIds.contains(toRemove)) {
          sender ! List("Could not find both climb ids").left
        } else {
          val event = ClimbsMerged(cmd.toKeep, cmd.toRemove)
          persist(event) { e =>
            updateState(e)
            sender ! cmd.toKeep.v.right
          }
        }
      }

      /*************************************************************************
       * Private classes
       *************************************************************************/

      /**
       * The state required to ensure data integrity.
       */

      private[this] case class State(
          cragIds: Set[CragId] = Set(),
          concreteClimbIds: Set[ClimbId] = Set()) {

        def addCrag(id: CragId) = copy(cragIds = cragIds + id)
        def removeCrag(id: CragId) = copy(cragIds = cragIds - id)
        def addClimb(id: ClimbId) = copy(concreteClimbIds = concreteClimbIds + id)
        def removeClimb(id: ClimbId) = copy(concreteClimbIds = concreteClimbIds - id)
      }
    }
  }
}
