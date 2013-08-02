package org.freeclimbers.api.controllers

import scala.concurrent.{Future, ExecutionContext}

import spray.http.HttpResponse
import spray.http.HttpHeader
import spray.http.StatusCode
import spray.http.StatusCodes._

import org.freeclimbers.core.dal.ClimbRepository
import org.freeclimbers.core.models.Climb
import org.freeclimbers.core.services.{ServiceFailure,ServiceEvent}
import org.freeclimbers.api.{PageLimits, Page, PageLinker}

trait ClimbControllerComponent {
  def climbController: ClimbController
}

trait ClimbController {

  type Response[T] = Future[(StatusCode, List[HttpHeader], T)]

  def getPage(paging: PageLimits, urlFor: PageLinker): Response[Page[Climb]]
  def getClimb(id: Long): Future[HttpResponse]
  def updateClimb(id: Long, newValue: Climb): Future[HttpResponse]
  def moveClimb(id: Long, cragId: Long): Future[HttpResponse]
  def latestChanges: Future[HttpResponse]
  def changes(page: Long): Future[HttpResponse]
}

class DefaultClimbController(
    ec: ExecutionContext,
    climbRepo: ClimbRepository) extends ClimbController {

  private implicit val _ec = ec

  override def getPage(paging: PageLimits, urlFor: PageLinker) = {
    for {
      (climbs, count) <- climbRepo.getPage(paging.limit, paging.offset)
    } yield (OK, Nil, Page(count, climbs, paging, urlFor))
  }

  override def getClimb(id: Long) = ???
  override def updateClimb(id: Long, newValue: Climb) = ???
  override def moveClimb(id: Long, cragId: Long) = ???
  override def latestChanges = ???
  override def changes(page: Long) = ???

}
