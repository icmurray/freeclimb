package org.freeclimbers.core.dal

import scala.concurrent._

import org.freeclimbers.core.models.Climb

trait ClimbRepositoryComponent {

  def climbRepo: ClimbRepository

}

trait ClimbRepository {
  
  def getPage(limit: Long, offset: Long): Future[(Seq[Climb], Long)]

}

class DefaultClimbRepository extends ClimbRepository {

  override def getPage(limit: Long, offset: Long) = {
    import ExecutionContext.Implicits.global
    future {
      (List(Climb("climb-1"), Climb("climb-2")), 2)
    }
  }

}
