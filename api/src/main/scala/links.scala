package org.freeclimbers
package api

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import org.freeclimbers.core.{CragId, Crag}

case class CragLink(
    id: CragId,
    href: String)

object CragLink extends SupportJsonFormats {

  def apply(crag: CragId): CragLink = CragLink(crag, href(crag))

  implicit val asJson = jsonFormat(CragLink.apply _, "id", "href")
  private[this] def href(crag: CragId) = s"/crags/${crag.uuid.toString}"
}

case class CragClimbsListingLink(href: String)
object CragClimbsListingLink extends SupportJsonFormats {
  def ofCrag(crag: CragId): CragClimbsListingLink = CragClimbsListingLink(href(crag))

  implicit val asJson = jsonFormat(CragClimbsListingLink.apply _, "href")
  private[this] def href(crag: CragId) = s"/crags/${crag.uuid.toString}/climbs"
}
