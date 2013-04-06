package org.freeclimbers.core.dal

trait DataAccessLayer extends ClimbRepositoryComponent

trait DefaultDataAccessLayer extends DataAccessLayer {

  override lazy val climbRepo = new DefaultClimbRepository()

}
