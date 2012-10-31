package freeclimb.test.sql

import com.googlecode.flyway.core.Flyway

import org.postgresql.ds._

import freeclimb.api._
import freeclimb.sql._

object TestDatabaseSessions {
  Class.forName("org.postgresql.Driver")
  
  // TODO: move info configuration file
  private val source = freeclimb.sql.createConnectionPool(
      "localhost",
      "freeclimb-test",
      "freeclimb",
      "testpassword",
      10)

  freeclimb.sql.performMigrations(source)

  def newSession[I <: IsolationLevel](isolationLevel: I) = new DbSession[I] {
    override val dbConnection = source.getConnection()
    override val level = isolationLevel
  }

  def newSession(): DbSession[TransactionReadCommitted] = newSession(TransactionReadCommitted)
}
