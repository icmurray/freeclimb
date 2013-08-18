package org.freeclimbers.core.queries

import scala.language.postfixOps

import scala.concurrent.stm._

import org.freeclimbers.core._
import org.freeclimbers.core.util.RichMap._

trait Climbs {
  def get(id: ClimbId): Option[Climb]
  def applyEvent(e: ClimbEvent): Unit
}

case class Climb(
    val climbId: ClimbId,
    val cragId: CragId,
    val name: String,
    val description: String)

/**
 * Thread-safe default implementation of Climbs based on a
 * privately held STM Reference.
 */
class DefaultClimbs extends Climbs {

  private val readModel = Ref(ClimbsReadModel())

  def get(id: ClimbId): Option[Climb] = atomic { implicit txn =>
    readModel().get(id)
  }

  def applyEvent(event: ClimbEvent) = atomic { implicit txn =>
    readModel.transform { _.applyEvent(event) }
  }
}

object DefaultClimbs {
  def fromHistory(events: Seq[ClimbEvent]): Climbs = {
    val climbs = new DefaultClimbs()
    events foreach { climbs.applyEvent(_) }
    climbs
  }
}

/**
 * An immutable utility class that provides the logic
 * of how ClimbEvents affect the read model.
 */
private case class ClimbsReadModel(
    private val byId: Map[ClimbId, Climb] = Map()) {

  def get(id: ClimbId): Option[Climb] = byId.get(id)

  def applyEvent(event: ClimbEvent) = event match {

    case e@ClimbCreated(climbId, _, _, _) =>
      this withClimb(climbId) createdBy {
        Climb(e.climbId, e.cragId, e.name, e.description)
      }

    case e@ClimbEdited(climbId, _, _) =>
      this withClimb(climbId) editedBy { climb =>
        climb.copy(name=e.name, description=e.description)
      }

    case e@ClimbMovedCrag(climbId, _, toCragId) =>
      this withClimb(climbId) editedBy { climb =>
        climb.copy(cragId=toCragId)
      }

    case e@ClimbDeleted(climbId) =>
      this withClimb(climbId) deleted
  }

  /**
   * Private DSL implementation.
   */
  private case class withClimb(id: ClimbId) {

    def editedBy(f: Climb => Climb): ClimbsReadModel = {
      ClimbsReadModel.this.copy(byId = byId.adjust(id)(f))
    }

    def createdBy(f: => Climb): ClimbsReadModel = {
      ClimbsReadModel.this.copy(byId = byId.updated(id, f))
    }

    def deleted = ClimbsReadModel.this.copy(byId = (byId - id))
  }

}

