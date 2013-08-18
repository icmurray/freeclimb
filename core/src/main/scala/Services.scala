package org.freeclimbers.core

import scala.util.{Try, Success, Failure}

import org.freeclimbers.core.eventstore._
import org.freeclimbers.core.queries.{DefaultClimbs, Climbs, Climb}

trait ClimbServices {

  def listClimbs(from: Int, to: Int): Seq[Climb]
  def getClimb(id: ClimbId): Option[Climb]

  def createClimb(cragId: CragId,
                  name: String,
                  description: String): Try[ClimbId]

  def deleteClimb(id: ClimbId, expected: Revision): Try[Unit]

  def updateClimb(id: ClimbId,
                  expected: Revision,
                  name: String,
                  description: String): Try[Unit]

  def moveClimb(climbId: ClimbId,
                expected: Revision,
                toCragId: CragId): Try[Unit]

}

trait ClimbServiceComponent {

  protected def climbs: Climbs
  protected def climbStore: EventStore[ClimbEvent]

  climbStore.subscribe(StoreRevision.initial) { commit =>
    commit.events.foreach { climbs.applyEvent(_) }
  }

  class ClimbServicesImpl extends ClimbServices {

    override def listClimbs(from: Int, to: Int) = {
      climbs.list.sortBy(_.name).view(from, to)
    }

    override def getClimb(id: ClimbId) = climbs.get(id)

    override def createClimb(cragId: CragId,
                             name: String,
                             description: String) = {

      val climbId = ClimbId.generate()
      val event = ClimbCreated(
        climbId = climbId,
        cragId = cragId,
        name = name,
        description = description)

      val result = climbStore.appendEvent(
        stream = getStreamId(climbId),
        expected = Revision.initial,
        event = event)

      result match {
        case c: Commit[_] => Success(climbId)
        case c: Conflict[_] => Failure(conflict)
      }
    }

    override def deleteClimb(id: ClimbId, expected: Revision) = {
      val result = climbStore.appendEvent(
        stream = getStreamId(id),
        expected = expected,
        event = ClimbDeleted(id))
      result match {
        case _: Commit[_]   => Success(())
        case _: Conflict[_] => Failure(conflict)
      }
    }

    override def updateClimb(id: ClimbId,
                             expected: Revision,
                             name: String,
                             description: String) = {
      val result = climbStore.appendEvent(
        stream = getStreamId(id),
        expected = expected,
        event = ClimbEdited(id, name, description))
      result match {
        case _: Commit[_]   => Success(())
        case _: Conflict[_] => Failure(conflict)
      }
    }

    override def moveClimb(id: ClimbId,
                           expected: Revision,
                           toCragId: CragId) = {


      val oldCragIdO = getClimb(id).map(_.cragId)
      oldCragIdO match {
        case None   => Failure(new RuntimeException(s"Unknown climb id: ${id}"))
        case Some(oldCragId) => {
          val result = climbStore.appendEvent(
            stream = getStreamId(id),
            expected = expected,
            event = ClimbMovedCrag(id, oldCragId, toCragId))
          result match {
            case _: Commit[_]   => Success(())
            case _: Conflict[_] => Failure(conflict)
          }
        }
      }
    }

    private def getStreamId(id: ClimbId) = {
      StreamId(id.uuid.toString)
    }

    private def conflict = new RuntimeException("Conflict")

  }
}

class DefaultClimbServiceComponent extends ClimbServiceComponent {
  override lazy val climbs = new DefaultClimbs()
  override lazy val climbStore: EventStore[ClimbEvent]  = ???
}

