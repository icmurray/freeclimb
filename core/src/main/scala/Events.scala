package org.freeclimbers.core

abstract class Event
sealed abstract class CoreEvent

sealed abstract class CragEvent extends CoreEvent {
  def cragId: CragId
}

sealed abstract class ClimbEvent extends CoreEvent {
  def climbId: ClimbId
}

case class ClimbCreated(
    val climbId: ClimbId,
    val cragId: CragId,
    val name: String,
    val description: String) extends ClimbEvent

case class ClimbEdited(
    val climbId: ClimbId,
    val name: String,
    val description: String) extends ClimbEvent

case class ClimbDeleted(
    val climbId: ClimbId) extends ClimbEvent

case class ClimbMovedCrag(
    val climbId: ClimbId,
    val fromCragId: CragId,
    val toCragId: CragId) extends ClimbEvent

case class CragCreated(
    val cragId: CragId,
    val name: String,
    val title: String) extends CragEvent

case class CragEdited(
    val cragId:  CragId,
    val name: String,
    val title: String) extends CragEvent

case class CragDeleted(
    val cragId: CragId) extends CragEvent
