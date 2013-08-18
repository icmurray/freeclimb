package org.freeclimbers.core.eventstore

case class Revision(value: Long) {
  def next = Revision(value + 1)
}

object Revision {
  val initial = Revision(1)
}
