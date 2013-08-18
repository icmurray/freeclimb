package org.freeclimbers.core.queries

import scala.util.Random

import org.scalacheck._, Arbitrary.arbitrary, Prop.forAll

import org.freeclimbers.core._

object ClimbsSpecification extends Properties("Climbs") {

  property("applyEvent") = {
    val climbId = ClimbId.generate()
    forAll(arbitrary(eventsForSingleClimb(climbId))) { events =>
      val climbs = DefaultClimbs.fromHistory(events)
      events.last match {
        case ClimbCreated(_, cragId, name, desc) =>
          climbs.get(climbId) == Some(Climb(climbId, cragId, name, desc))
          climbs.list == Seq(Climb(climbId, cragId, name, desc))
        case ClimbEdited(_, name, desc) => {
          climbs.get(climbId).get.name        == name &&
          climbs.get(climbId).get.description == desc &&
          climbs.list.head.name               == name &&
          climbs.list.head.description        == desc
        }
        case ClimbDeleted(_) =>
          climbs.get(climbId) == None &&
          climbs.list.isEmpty
        case ClimbMovedCrag(_, _, toCragId) =>
          climbs.get(climbId).get.cragId == toCragId &&
          climbs.list.head.cragId        == toCragId
      }
    }
  }

  implicit lazy val arbitraryCragId: Arbitrary[CragId] = {
    Arbitrary(Gen.wrap(CragId.generate))
  }

  def eventsForSingleClimb(id: ClimbId): Arbitrary[List[ClimbEvent]] = {
    Arbitrary(
      for {
        created  <- Gen.resultOf(
          ClimbCreated(id, _: CragId, _: String, _: String))
        editions <- Gen.listOf(Gen.resultOf(
          ClimbEdited(id, _: String, _: String)))
        movements <- Gen.listOf(Gen.resultOf(
          ClimbMovedCrag(id, _: CragId, _: CragId)))
        deleted   <- Gen.frequency(3 -> Nil,
                                   1 -> List(ClimbDeleted(id)))
      } yield (created ::
               Random.shuffle(editions ::: movements) :::
               deleted)
    )
  }

}
