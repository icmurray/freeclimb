package freeclimb.test.sql

import com.googlecode.flyway.core.Flyway

import org.postgresql.ds._

import freeclimb.api._
import freeclimb.sql._

object TestDatabaseSessions {
  Class.forName("org.postgresql.Driver")
  
  // TODO: move info configuration file
  val source = freeclimb.sql.createConnectionPool(
      "localhost",
      "freeclimb-test",
      "freeclimb",
      "testpassword",
      10)

  freeclimb.sql.performMigrations(source)

  def newConnection(level: IsolationLevel) = {
    val c = source.getConnection()
    c.setAutoCommit(false)
    c.setTransactionIsolation(level.jdbcLevel)
    c
  }

}
