package org.freeclimbers
package core

import scala.concurrent._
import scala.concurrent.duration._

import akka.persistence._
import akka.persistence.journal._
import akka.actor.{ActorSystem, Actor}
import akka.testkit.TestKit

import com.typesafe.config.ConfigFactory

trait TestUtils extends TestExecutionUtils
                   with TestEventsourcedUtils

trait TestExecutionUtils {

  def blockFor[T](f: => Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  def blockFor[T](f: => ValidatedT[Future, T]): Validated[T] = {
    Await.result(f.run, 2.seconds)
  }

  def runCommand[T](f: => ValidatedT[Future, T])(implicit ec: ExecutionContext): T = {
    blockFor {
      f.run.flatMap(_.fold(
        left  => future { throw new Exception(left.toString) },
        right => future { right }
      ))
    }
  }

}

trait TestEventsourcedUtils {

  lazy val unitTestConfig = {
    val journalConfig = ConfigFactory.parseString(
    """
    {
      akka.persistence.journal.plugin = "null_journal"

      null_journal {
        class = "org.freeclimbers.core.NullJournal"
        plugin-dispatcher = "akka.actor.default-dispatcher"
      }

      akka.log-dead-letters-during-shutdown = off
    }
    """)

    journalConfig withFallback ConfigFactory.load()
  }

}

class NullJournal extends Actor with SyncWriteJournal {
  import scala.concurrent.ExecutionContext.Implicits.global
  def write(persistent: PersistentImpl): Unit = {}
  def writeBatch(persistentBatch: Seq[PersistentImpl]): Unit = {}
  def delete(persistent: PersistentImpl): Unit = {}
  def confirm(processorId: String, sequenceNr: Long, channelId: String): Unit = {}
  def replayAsync(processorId: String, fromSequenceNr: Long, toSequenceNr: Long)
                 (replayCallback: PersistentImpl => Unit)
                 : Future[Long] = future { 0L }
}

