package org.freeclimbers.core.dal

import org.freeclimbers.core.models.Climb

trait ClimbRepositoryComponent {

  def climbRepo: ClimbRepository

}

trait ClimbRepository {
  
  def getPage(limit: Long, offset: Long): (Seq[Climb], Long)

}
