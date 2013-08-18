package org.freeclimbers.core.eventstore

case class StoreRevision(value: Long) {
  def next = StoreRevision(value + 1)
}

object StoreRevision {
  val initial = StoreRevision(1L)
}


