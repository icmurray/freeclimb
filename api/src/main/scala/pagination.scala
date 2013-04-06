package org.freeclimbers.api

case class PaginationRequest(
    limit: Long,
    offset: Long) {

  require(limit > 0, "limit must be positive")
  require(offset >= 0, "offset must be non-negative")
}

case class PagedResponse[T](
    count: Long,
    payload: Seq[T])

