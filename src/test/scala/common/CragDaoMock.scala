package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql.CragDao

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
        crags += name -> rev
        updated(rev.model, revision).right
      } else {
        EditConflict().left
      }
    } else {
      NotFound().left
    }
  }

  def reset() {
    revision = 0L
    crags = Map()
  }

}

