package org.freeclimbers.api.controllers

import scala.concurrent.{Future, ExecutionContext}

import org.freeclimbers.core.dal.ClimbRepository
import org.freeclimbers.core.models.Climb
import org.freeclimbers.api.{PageLimits, Page, PageLinker}

trait ClimbControllerComponent {
  def climbController: ClimbController
}

trait ClimbController {
  def getPage(paging: PageLimits, urlFor: PageLinker): Future[Page[Climb]]
}

class DefaultClimbController(
    ec: ExecutionContext,
    climbRepo: ClimbRepository) extends ClimbController {

  private implicit val _ec = ec

  override def getPage(paging: PageLimits, urlFor: PageLinker): Future[Page[Climb]] = {
    for {
      (climbs, count) <- climbRepo.getPage(paging.limit, paging.offset)
    } yield Page(count, climbs, paging, urlFor)
  }

}
