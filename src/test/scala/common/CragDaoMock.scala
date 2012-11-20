package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

import freeclimb.test.sql.CragDaoSpec

class CragDaoMock extends CragDao {


  private var revision: Long = 0L
  private var crags: Map[String, NonEmptyList[Revisioned[Crag]]] = Map()
  private var deletedCrags: Map[String, Revisioned[Crag]] = Map()

  reset()

  override def create(crag: Crag) = ApiAction { session =>

    if (crags.contains(crag.name)) {
      EditConflict().left
    } else {
      revision += 1
      val rev = Revisioned(revision, crag)
      crags += crag.name -> NonEmptyList(rev)
      created(crag, revision).right
    }
  }

  override def getOption(name: String) = ApiReadAction { session =>
    crags.get(name) map { _.head } right
  }

  override def list() = ApiReadAction { session =>
    crags.values.toList map { _.head.model } right
  }

  override def update(rev: Revisioned[Crag]) = ApiAction { session =>
    val name = rev.model.name
    if (crags.contains(name)) {
      val currentRev = crags(name).head

      if (currentRev.revision == rev.revision) {
        revision += 1
        val tail: List[Revisioned[Crag]] = if (currentRev.model == rev.model) {
          crags(name).tail.toList
        } else {
          crags(name).toList
        }

        crags += name -> NonEmptyList(Revisioned[Crag](revision, rev.model), tail: _*)
        updated(rev.model, revision).right
      } else {
        EditConflict().left
      }
    } else {
      NotFound().left
    }
  }

  override def history(crag: Crag) = ApiReadAction { session =>
    val name = crag.name
    crags.get(name) map { _.toList.right } getOrElse List().right
  }

  override def deletedList() = ApiReadAction { session =>
    deletedCrags.values.toList.right
  }

  override def purge(crag: Revisioned[Crag]) = ApiAction { session =>
    val name = crag.model.name
    if (crags contains name) {

      if (crags(name).head.revision != crag.revision) {
        EditConflict().left
      } else {
        crags -= name
        deletedCrags -= name
        purged(crag).right
      }
    } else {
      NotFound().left
    }
  }

  override def delete(crag: Revisioned[Crag]) = ApiAction { session =>
    val name = crag.model.name
    if (crags contains name) {
      val rev = crags(name).head

      if (rev.revision != crag.revision) {
        EditConflict().left
      } else {
        deletedCrags += name -> crags(name).head
        crags -= crag.model.name
        deleted(rev).right
      }
    } else {
      NotFound().left
    }
  }

  private def TODO: Nothing = throw new UnsupportedOperationException("Not implemented")

  def reset() {
    revision = 0L
    crags = Map()
    deletedCrags = Map()
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
