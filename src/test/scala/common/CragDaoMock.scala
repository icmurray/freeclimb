package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

import freeclimb.test.sql.CragDaoSpec

class CragDaoMock(db: FakeDb) extends CragDao {

  override def create(crag: Crag) = db.createCrag(crag)

  override def getOption(name: String) = db.getOption(name)

  override def list() = db.listCrags

  override def update(rev: Revisioned[Crag]) = db.updateCrag(rev)

  override def history(crag: Crag) = db.history(crag)

  override def deletedList() = db.deletedList

  override def purge(crag: Revisioned[Crag]) = db.purgeCrag(crag)

  override def delete(crag: Revisioned[Crag]) = db.deleteCrag(crag)

}

class CragDaoMockTest extends CragDaoSpec {

  override protected val runner = new ActionRunner {
    def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](action: ActionT[M,A,I,W])
                                                                (implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A] = {
      val session = new DbSession[I] { val dbConnection = null }
      val result = action(session)

      // Discard the logged events from the final result
      result map { _._2 }
    }
  }

  override def withCragDao(testCode: CragDao => Any) {
    val db = new FakeDb()
    val cragDao = new CragDaoMock(db)
    testCode(cragDao)
  }

  override def withCragAndClimbDaos(testCode: (CragDao, ClimbDao) => Any) {
    val db = new FakeDb()
    val cragDao = new CragDaoMock(db)
    val climbDao = new FakeClimbDao(db)
    testCode(cragDao, climbDao)
  }

}
