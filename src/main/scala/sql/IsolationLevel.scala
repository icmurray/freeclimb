package freeclimb.sql

import java.sql.Connection

object IsolationLevel extends Enumeration {
  type IsolationLevel = Value

  /** Define them in order of strictness. */
  val TransactionNone            = Value(0, "TRANSACTION_NONE")
  val TransactionReadUnCommitted = Value(1, "TRANSACTION_READ_UNCOMMITTED")
  val TransactionReadCommitted   = Value(2, "TRANSACTION_READ_COMMITTED")
  val TransactionRepeatableRead  = Value(3, "TRANSACTION_REPEATABLE_READ")
  val TransactionSerializable    = Value(4, "TRANSACTION_SERIALIZABLE")

  def toJdbcLevel(level: IsolationLevel): Int = level match {
    case TransactionNone            => Connection.TRANSACTION_NONE
    case TransactionReadUnCommitted => Connection.TRANSACTION_READ_UNCOMMITTED
    case TransactionReadCommitted   => Connection.TRANSACTION_READ_COMMITTED
    case TransactionRepeatableRead  => Connection.TRANSACTION_REPEATABLE_READ
    case TransactionSerializable    => Connection.TRANSACTION_SERIALIZABLE
  }
}
