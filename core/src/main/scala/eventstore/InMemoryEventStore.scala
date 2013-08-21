package org.freeclimbers.core.eventstore

import scala.annotation.tailrec

import java.util.concurrent.{Executors, ExecutorService}

import org.joda.time.DateTime

import scala.concurrent.stm._

import org.freeclimbers.core.util.RichMap._

class InMemoryEventStore[E] extends EventStore[E] {

  private val events: Ref[IndexedSeq[Commit[E]]] = Ref(Vector())
  private val streamEvents: TMap[StreamId, IndexedSeq[Commit[E]]] = TMap()
  private val pool: ExecutorService = Executors.newFixedThreadPool(5)

  override def appendEvent(stream: StreamId,
                           expected: Revision,
                           event: E): AppendResult[E] = {

    atomic { implicit txn =>
      val revision = streamRevision(stream)

      if (revision == expected) {
        val commit = Commit(storeRevision.next,
                            stream,
                            revision.next,
                            List(event))

        events.transform { _ :+ commit }
        streamEvents.updated(
          stream,
          streamEvents.getOrElse(stream, Vector()) :+ commit)

        commit
      } else if (revision.value > expected.value) {
        val conflicting = streamCommits(stream, expected, revision)
        Conflict(stream,
                 revision,
                 expected,
                 conflicting)
      } else {
        throw new IllegalArgumentException(
          s"Expected revision '${expected.value}' greater than " +
          s"current revision '${revision.value}'")
      }
    }
  }

  override def subscribe(since: StoreRevision)
                        (callback: Commit[E] => Unit): Subscription = {

    val cancelled = Ref(false)
    val latest = Ref(since)

    val notifier = new Runnable {

      @tailrec
      override def run(): Unit = {

        val pending: Option[Stream[Commit[E]]] = atomic { implicit txn =>
          val current = storeRevision
          if (latest() == current) { retry }
          else if(cancelled()) { None }
          else { Some(allCommits(latest(), current)) }
        }

        pending match {
          case Some(commits) => commits.foreach(callback) ; run()
          case None          => // fall out of loop
        }
      }
    }
    pool.execute(notifier)

    new Subscription {
      override def cancel(): Unit = atomic { implicit txn =>
        cancelled() = true
      }
    }

  }

  override def storeRevision: StoreRevision = atomic { implicit txn =>
    StoreRevision(events().length)
  }

  override def streamRevision(stream: StreamId): Revision = {
    atomic { implicit txn =>
      streamEvents.get(stream)
                  .map(_.length)
                  .map(Revision(_))
                  .getOrElse(Revision.initial)
    }
  }

  override def allCommits(from: StoreRevision,
                          to: StoreRevision): Stream[Commit[E]] = {
    atomic { implicit txn =>
      events().slice(from.value.toInt, to.value.toInt).toStream
    }
  }

  override def streamCommits(stream: StreamId,
                             from: Revision,
                             to: Revision): Stream[Commit[E]] = {
    atomic { implicit txn =>
      streamEvents.getOrElse(stream, Vector())
                  .slice(from.value.toInt, to.value.toInt)
                  .toStream
    }
  }
}
