package org.freeclimbers.core.eventstore

trait Subscription {
  def cancel: Unit
}
