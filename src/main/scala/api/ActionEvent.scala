package freeclimb.api

import freeclimb.models._

sealed trait ActionEvent

case class CragCreated(val crag: Revisioned[Crag]) extends ActionEvent
case class CragUpdated(val crag: Revisioned[Crag]) extends ActionEvent
case class CragDeleted(val crag: Revisioned[Crag]) extends ActionEvent
case class CragPurged(val crag: Revisioned[Crag]) extends ActionEvent

case class ClimbCreated(val climb: Revisioned[Climb]) extends ActionEvent
case class ClimbUpdated(val climb: Revisioned[Climb]) extends ActionEvent
case class ClimbDeleted(val climb: Revisioned[Climb]) extends ActionEvent
case class ClimbPurged(val climb: Revisioned[Climb]) extends ActionEvent
