package freeclimb.test.common

import freeclimb.api._
import freeclimb.models._
import freeclimb.sql._

class FakeClimbDao(db: FakeDb) extends ClimbDao {

  override def create(climb: Climb) = db.createClimb(climb)

  override def getOption(crag: String, name: String) = db.getOption(crag, name)

  override def update(rev: Revisioned[Climb]) = TODO

  override def history(climb: Climb) = TODO

  override def deletedList() = TODO

  override def purge(climb: Revisioned[Climb]) = TODO

  override def delete(climb: Revisioned[Climb]) = TODO

  private def TODO: Nothing = throw new UnsupportedOperationException("Not implemented")
}

