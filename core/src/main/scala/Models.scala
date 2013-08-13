package org.freeclimbers.core

import java.util.UUID

case class ClimbId(uuid: UUID)
object ClimbId {
  def generate() = ClimbId(UUID.randomUUID())
}

case class CragId(uuid: UUID)
object CragId {
  def generate() = CragId(UUID.randomUUID())
}

