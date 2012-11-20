package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

import freeclimb.test.sql.CragDaoSpec

class CragDaoMock extends CragDao {

  private var revision: Long = 0L
  private var crags: Map[String, Revisioned[Crag]] = Map()

  reset()

  override def create(crag: Crag) = ApiAction { session =>

    if (crags.contains(crag.name)) {
      EditConflict().left
    } else {
      revision += 1
      val rev = Revisioned(revision, crag)
      crags += crag.name -> rev
      created(crag, revision).right
    }
  }

  override def getOption(name: String) = ApiReadAction { session =>
    crags.get(name).right
  }

  override def list() = ApiReadAction { session =>
    crags.values.toList map { _.model } right
  }

  override def update(rev: Revisioned[Crag]) = ApiAction { session =>
    val name = rev.model.name
    if (crags.contains(name)) {
      val currentRev = crags.get(name).get
      if (currentRev.revision == rev.revision) {
        revision += 1
        crags += name -> Revisioned[Crag](revision, rev.model)
        updated(rev.model, revision).right
      } else {
        EditConflict().left
      }
    } else {
      NotFound().left
    }
  }

  override def history(crag: Crag) = TODO
  override def deletedList() = TODO
  override def purge(crag: Revisioned[Crag]) = TODO
  override def delete(crag: Revisioned[Crag]) = TODO

  private def TODO: Nothing = throw new UnsupportedOperationException("Not implemented")

  def reset() {
    revision = 0L
    crags = Map()
  }

}

class CragDaoMockTest extends CragDaoSpec {

  override protected val cragDao = new CragDaoMock()
  override protected val runner = new ActionRunner {
    def run[M[+_],A,I <: IsolationLevel, W <: List[ActionEvent]](action: ActionT[M,A,I,W])
                                                                (implicit F: Failable[M[_]], M: Functor[M], m: Manifest[I]): M[A] = {
      val session = new DbSession[I] { val dbConnection = null }
      val result = action(session)

      // Discard the logged events from the final result
      result map { _._2 }
    }
  }

  override def cleanDao() {
    cragDao.reset()
  }

}
