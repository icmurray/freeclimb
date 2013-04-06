package org.freeclimbers.api.controllers

import scala.concurrent.{Future, ExecutionContext}

import org.freeclimbers.core.dal.ClimbRepository
import org.freeclimbers.core.models.Climb
import org.freeclimbers.api.{PaginationRequest, PagedResponse}

trait ClimbControllerComponent {
  def climbController: ClimbController
}

trait ClimbController {
  def getPage(paging: PaginationRequest): Future[PagedResponse[Climb]]
}

class DefaultClimbController(
    ec: ExecutionContext,
    climbRepo: ClimbRepository) extends ClimbController {

  private implicit val _ec = ec

  def getPage(paging: PaginationRequest): Future[PagedResponse[Climb]] = {
    for {
      (climbs, count) <- climbRepo.getPage(paging.limit, paging.offset)
    } yield PagedResponse(count, climbs)
  }

}
