package org.freeclimbers.core.eventstore

trait EventStore[E] {

  def appendEvent(stream: StreamId,
                  expected: Revision,
                  event: E): AppendResult[E]

  def subscribe(since: StoreRevision)
               (callback: Commit[E] => Unit): Subscription

  def storeRevision: StoreRevision
  def streamRevision(stream: StreamId): Revision
  def allCommits(from: StoreRevision, to: StoreRevision): Stream[Commit[E]]
  def streamCommits(stream: StreamId, from: Revision, to: Revision): Stream[Commit[E]]
}
