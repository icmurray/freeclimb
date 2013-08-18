package org.freeclimbers.core

import scala.util.{Try, Success, Failure}

import org.freeclimbers.core.queries.{DefaultClimbs, Climbs, Climb}

trait ClimbServices {

  def listClimbs(from: Int, to: Int): Seq[Climb]
  def getClimb(id: ClimbId): Option[Climb]

  def createClimb(cragId: CragId,
                  name: String,
                  description: String): Try[ClimbId]

  def deleteClimb(id: ClimbId): Try[Unit]

  def updateClimb(id: ClimbId,
                  name: String,
                  description: String): Try[Unit]

  def moveClimb(climbId: ClimbId,
                toCragId: CragId): Try[Unit]

}

trait ClimbServiceComponent {
  def climbs: Climbs

  class ClimbServicesImpl extends ClimbServices {

    override def listClimbs(from: Int, to: Int) = {
      val sorted = climbs.list.sortBy(_.name)
      sorted.view(from, to)
    }

    override def getClimb(id: ClimbId) = climbs.get(id)

    override def createClimb(cragId: CragId,
                             name: String,
                             description: String) = {
      val climbId = ClimbId.generate()
      climbs.applyEvent(ClimbCreated(
        climbId = climbId,
        cragId = cragId,
        name = name,
        description = description))
      Success(climbId)
    }

    override def deleteClimb(id: ClimbId) = {
      Success(climbs.applyEvent(ClimbDeleted(id)))
    }

    override def updateClimb(id: ClimbId,
                             name: String,
                             description: String) = {
      Success(climbs.applyEvent(ClimbEdited(id, name, description)))
    }

    override def moveClimb(id: ClimbId, toCragId: CragId) = {
      val oldCragIdO = getClimb(id).map(_.cragId)
      oldCragIdO match {
        case None   => Failure(new RuntimeException(
                                s"Unknown climb id: ${id}"))
        case Some(oldCragId) => Success(
          climbs.applyEvent(ClimbMovedCrag(id, oldCragId, toCragId))
        )
      }
    }
  }
}

class DefaultClimbServiceComponent extends ClimbServiceComponent {
  override val climbs = new DefaultClimbs()
}

