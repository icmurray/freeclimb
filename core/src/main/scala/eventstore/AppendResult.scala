package org.freeclimbers.core.eventstore

import org.joda.time.DateTime

sealed trait AppendResult[+E] {
  def streamId: StreamId
}

case class Commit[+E](
    timestamp: DateTime,
    storeRevision: StoreRevision,
    streamId: StreamId,
    streamRevision: Revision,
    events: Seq[E]) extends AppendResult[E]

case class Conflict[+E](
    streamId: StreamId,
    actualRevision: Revision,
    expectedRevision: Revision,
    conflicts: Seq[Commit[E]]) extends AppendResult[E]

