package freeclimb.sql

import java.sql.Connection

sealed trait IsolationLevel {
  val jdbcLevel: Int
}

trait TransactionNone extends IsolationLevel {
  override val jdbcLevel = Connection.TRANSACTION_NONE
}

trait TransactionReadUncommitted extends TransactionNone {
  override val jdbcLevel = Connection.TRANSACTION_READ_UNCOMMITTED
}

trait TransactionReadCommitted extends TransactionReadUncommitted {
  override val jdbcLevel = Connection.TRANSACTION_READ_COMMITTED
}

trait TransactionRepeatableRead extends TransactionReadCommitted {
  override val jdbcLevel = Connection.TRANSACTION_REPEATABLE_READ
}

trait TransactionSerializable extends TransactionRepeatableRead {
  override val jdbcLevel = Connection.TRANSACTION_SERIALIZABLE
}

object TransactionNone extends TransactionNone
object TransactionReadUncommitted extends TransactionReadUncommitted
object TransactionReadCommitted extends TransactionReadCommitted
object TransactionRepeatableRead extends TransactionRepeatableRead
object TransactionSerializable extends TransactionSerializable

