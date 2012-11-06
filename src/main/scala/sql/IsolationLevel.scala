package freeclimb.sql

import java.sql.Connection

sealed trait IsolationLevel {
  val jdbcLevel: Int
}

class TransactionNone extends IsolationLevel {
  override val jdbcLevel = Connection.TRANSACTION_NONE
}

class TransactionReadUncommitted extends TransactionNone {
  override val jdbcLevel = Connection.TRANSACTION_READ_UNCOMMITTED
}

class TransactionReadCommitted extends TransactionReadUncommitted {
  override val jdbcLevel = Connection.TRANSACTION_READ_COMMITTED
}

class TransactionRepeatableRead extends TransactionReadCommitted {
  override val jdbcLevel = Connection.TRANSACTION_REPEATABLE_READ
}

final class TransactionSerializable extends TransactionRepeatableRead {
  override val jdbcLevel = Connection.TRANSACTION_SERIALIZABLE
}
