package org.freeclimbers
package core

import java.util.UUID

import scala.language.higherKinds
import scala.concurrent.{Future, future}

import scalaz._
import Scalaz._

/*****************************************************************************
 * Models
 *****************************************************************************/

case class CragId(uuid: UUID) extends AnyVal
object CragId {
  def createRandom() = CragId(UUID.randomUUID())
}

case class Crag(
    id: CragId,
    name: String,
    description: String)


/*****************************************************************************
 * Events
 *****************************************************************************/

sealed trait CragEvent
case class CragCreated(
    id: CragId,
    name: String,
    description: String) extends CragEvent


/*****************************************************************************
 * Service Module
 *****************************************************************************/

/**
 * The Crag service interface definitions
 */
trait CragsModule[M[+_]] {
  implicit def M: Monad[M]

  val crags: CragService

  trait CragService {

    // commands
    def create(name: String): M[Validated[CragId]]

    // queries
    // Note - query results are only *eventually* consistent with issued commands.
    def withId(id: CragId): M[Option[Crag]]
  }

}

trait EventsourcedCragsModule extends CragsModule[Future] {
  this: ActorSystemModule =>

  lazy val crags = new Impl()

  class Impl extends CragService {
    def create(name: String) = ???
    def withId(id: CragId) = ???
  }

}
