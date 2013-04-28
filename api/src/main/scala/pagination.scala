package org.freeclimbers.api

import scala.math.max

case class PageLimits(
    limit: Long,
    offset: Long) {

  require(limit > 0, "limit must be positive")
  require(offset >= 0, "offset must be non-negative")
}

/**
 * Wraps a function that provides the URL for a fixed URI with the given paging limits.
 */
class PageLinker(f: PageLimits => String) extends Function1[PageLimits, String] {
  def apply(paging: PageLimits): String = f(paging)
}

case class Page[T](
    count: Long,
    payload: Seq[T],
    links: Map[String,String])

object Page {

  /**
   * A convenience function for constructing a `Page[T]` with links to itself, first, last
   * and optionally previous and next `Page[T]`s.
   */
  def apply[T](count: Long, payload: Seq[T], paging: PageLimits, linkTo: PageLinker): Page[T] = {
    val links = linksFor(count, paging, linkTo)
    Page(count, payload, links)
  }

  /**
   * A convenience function for constructing links.
   */
  def linksFor(count: Long, paging: PageLimits, linkTo: PageLinker): Map[String,String] = {
    val PageLimits(limit, offset) = paging

    var links = Map(
      "self"   -> linkTo(paging),
      "first"  -> linkTo(paging.copy(offset = 0)),
      "last"   -> linkTo(paging.copy(offset = max(0, count - limit))))

    if (offset >= limit)
      links += "prev" -> linkTo(paging.copy(offset = offset - limit))

    if (offset + limit <= count)
      links += "next" -> linkTo(paging.copy(offset = offset + limit))

    links
  }

}

