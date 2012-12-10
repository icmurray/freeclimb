package freeclimb.test.common

import freeclimb.api.CrudApi

class CrudApiMock extends CrudApi {

  private var db = new FakeDb()
  var _cragDao = new CragDaoMock(db)
  var _climbDao = new FakeClimbDao(db)
  override def cragDao = _cragDao
  override def climbDao = _climbDao

  def reset() {
    db = new FakeDb()
    _cragDao = new CragDaoMock(db)
    _climbDao = new FakeClimbDao(db)
  }
}
