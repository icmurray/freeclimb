package org.freeclimbers.core.dal

trait DataAccessLayer extends ClimbRepositoryComponent

trait DefaultDataAccessLayer extends DataAccessLayer {

  override lazy val climbRepo = new ClimbRepository {
    def getPage(limit: Long, offset: Long) = { sys.error("Not implemented") }
  }

}
