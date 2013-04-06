package org.freeclimbers.api

case class PaginationRequest(
    limit: Long,
    offset: Long) {

  require(limit > 0, "limit must be positive")
  require(offset >= 0, "offset must be non-negative")
}

case class PagedResponse[T](
    limit: Long,
    offset: Long,
    count: Long,
    payload: Seq[T])

object PagedResponse {
  def apply[T](paging: PaginationRequest, count: Long, payload: Seq[T]): PagedResponse[T] = {
    PagedResponse(
      limit = paging.limit,
      offset = paging.offset,
      count = count,
      payload = payload)
  }
}
