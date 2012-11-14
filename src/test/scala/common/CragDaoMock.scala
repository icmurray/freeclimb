package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql.CragDao

class CragDaoMock extends CragDao {

  private var revision: Long = 0L
  private var crags: Set[Revisioned[Crag]] = Set()

  reset()

  override def create(crag: Crag) = ApiAction { session =>
    revision += 1
    val rev = Revisioned(revision, crag)
    crags += rev
    created(crag, revision).right
  }

  override def getOption(name: String) = ApiReadAction { session =>
    crags.find(c => c.model.name == name).right
  }

  def reset() {
    revision = 0L
    crags = Set()
  }

}

