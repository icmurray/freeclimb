package freeclimb.test.common

import freeclimb.api.CrudApi

class CrudApiMock extends CrudApi {

  override val cragDao = new CragDaoMock()
  override val climbDao = null

  def reset() {
    cragDao.reset()
  }
}
