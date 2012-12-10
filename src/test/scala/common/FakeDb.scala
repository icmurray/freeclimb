package freeclimb.test.common

import scalaz._
import Scalaz._

import freeclimb.api._
import freeclimb.models._

class FakeDb {

  private var revision = 0L

  private var crags: Map[String, Revisioned[Crag]] = Map()
  private var cragHistory: Map[String, List[Revisioned[Crag]]] = Map()
  private var deletedCrags: Map[String, List[Revisioned[Crag]]] = Map()

  private var climbs: Map[(String, String), Revisioned[Climb]] = Map()
  private var climbHistory: Map[String, List[Revisioned[Climb]]] = Map()
  
  def createCrag(crag: Crag) = ApiUpdateAction { session =>
    if (crags contains crag.name) {
      EditConflict().left
    } else {
      val rev = newRevision(crag)
      crags += crag.name -> rev
      cragHistory += crag.name -> List(rev)
      created(rev).right
    }
  }

  def createClimb(climb: Climb) = ApiUpdateAction { session =>
    if (! crags.contains(climb.crag.name)) {
      ValidationError().left
    }
    else if (climbs contains (climb.crag.name, climb.name)) {
      EditConflict().left
    } else {
      val rev = newRevision(climb)
      climbs += (climb.crag.name, climb.name) -> rev
      climbHistory += climb.name -> List(rev)
      created(rev).right
    }
  }

  def getOption(name: String) = ApiReadAction { session =>
    crags.get(name).right
  }

  def getOption(crag: String, climb: String) = ApiReadAction { session =>
    climbs.get((crag, climb)).right
  }

  def listCrags = ApiReadAction { session =>
    crags.values.toList map { _.model } right
  }

  def deletedList = ApiReadAction { session =>
    deletedCrags.values.toList.flatten.right
  }

  def history(crag: Crag) = ApiReadAction { session =>
    val name = crag.name
    (cragHistory.get(name) getOrElse List()).right
  }

  def deleteCrag(rev: Revisioned[Crag]) = ApiUpdateAction { session =>
    val crag = rev.model
    val name = crag.name
    if(! crags.contains(name) ) {
      NotFound().left
    } else if (crags(name).revision != rev.revision) {
      EditConflict().left
    } else if (climbs.values map { _.model.crag.name } filter { _ == name } nonEmpty) {
      ValidationError().left
    } else {
      crags -= name
      cragHistory -= name
      deletedCrags = Map(name -> List(rev)) |+| deletedCrags
      deleted(rev).right
    }
  }

  def updateCrag(rev: Revisioned[Crag]) = ApiUpdateAction { session =>
    val crag = rev.model
    val name = crag.name
    if (! crags.contains(name) ) {
      NotFound().left
    } else if ( crags(name).revision != rev.revision ) {
      EditConflict().left
    } else {
      val newRev = newRevision(crag)

      val tail = if ( crag == crags(name).model ) {
        cragHistory(name).tail
      } else {
        cragHistory(name)
      }

      crags += crag.name -> newRev
      cragHistory += name -> (newRev :: tail)
      created(newRev).right
    }
  }

  def updateClimb(rev: Revisioned[Climb]) = ApiUpdateAction { session =>
    val climb = rev.model
    val climbId = (climb.crag.name, climb.name)
    if (! climbs.contains(climbId)) {
      NotFound().left
    } else if  ( climbs(climbId).revision != rev.revision ) {
      EditConflict().left
    } else {
      val newRev = newRevision(climb)
      climbs += climbId -> newRev

      created(newRev).right
    }
  }

  def purgeCrag(rev: Revisioned[Crag]) = ApiUpdateAction { session =>
    val crag = rev.model
    val name = crag.name
    if(! crags.contains(name) ) {
      NotFound().left
    } else if (crags(name).revision != rev.revision) {
      EditConflict().left
    } else if (climbs.values map { _.model.crag.name } filter { _ == name } nonEmpty) {
      ValidationError().left
    } else {
      crags -= name
      cragHistory -= name
      deletedCrags -= name
      purged(rev).right
    }
  }

  /**
   * Increment revision counter and construct new Revisioned[T]
   */
  private def newRevision[T](t: T): Revisioned[T] = {
    revision += 1
    Revisioned(revision, t)
  }

  /**
   * Helper methods.  Don't care about the ApiActions
   */
  private def created[T](rev: Revisioned[T]) = (Nil, rev)
  private def updated[T](rev: Revisioned[T]) = (Nil, rev)
  private def deleted[T](rev: Revisioned[T]) = (Nil, rev)
  private def purged[T](rev: Revisioned[T]) = (Nil, rev)

}
