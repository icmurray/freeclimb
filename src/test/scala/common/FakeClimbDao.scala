package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

import freeclimb.test.sql.ClimbDaoSpec

class FakeClimbDao(db: FakeDb) extends ClimbDao {

  override def create(climb: Climb) = db.createClimb(climb)

  override def getOption(crag: String, name: String) = db.getOption(crag, name)

  override def update(rev: Revisioned[Climb]) = db.updateClimb(rev)

  override def history(climb: Climb) = db.history(climb)

  override def deletedList() = db.deletedClimbList

  override def purge(climb: Revisioned[Climb]) = db.purgeClimb(climb)

  override def delete(climb: Revisioned[Climb]) = db.deleteClimb(climb)

  override def climbsCreatedOrUpdatedSince(crag: Revisioned[Crag]) = {
    db.climbsCreatedOrUpdatedSince(crag)
  }

  override def climbsDeletedSince(crag: Revisioned[Crag]) = {
    db.climbsDeletedSince(crag)
  }

  private def TODO: Nothing = throw new UnsupportedOperationException("Not implemented")
}

class FakeClimbDaoTest extends ClimbDaoSpec {

  override val runner = new ActionRunner {
    def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](action: ActionT[M,A,I,W])
                                                                (implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A] = {
      val session = new DbSession[I] { val dbConnection = null }
      val result = action(session)

      // Discard the logged events from the final result
      result map { _._2 }
    }
  }

  override def withDaos(testCode: (CragDao, ClimbDao) => Any) {
    val db = new FakeDb()
    val cragDao = new CragDaoMock(db)
    val climbDao = new FakeClimbDao(db)
    testCode(cragDao, climbDao)
  }


}
