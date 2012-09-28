package freeclimb.sql

import java.sql.SQLException

trait WithSqlError {
  val sqlError: Option[SqlError.SqlError]
}

object SqlError extends Enumeration {
  type SqlError = Value
  val NotNullViolation = Value("23502")
  val UniqueViolation = Value("23505")
  val CheckViolation = Value("23514")
  val SerializationFailure = Value("40001")

  implicit def sqlException2SqlError(e: SQLException): WithSqlError = new WithSqlError {
    val sqlError = SqlError.values.find(_.toString == e.getSQLState)
  }
}


