package freeclimb.test.sql

import com.googlecode.flyway.core.Flyway

import org.postgresql.ds._

import freeclimb.api._
import freeclimb.sql._

object TestDatabaseSessions {
  Class.forName("org.postgresql.Driver")

  private val source = new PGPoolingDataSource();
  source.setDataSourceName("Test datasource.")
  source.setServerName("localhost");
  source.setDatabaseName("freeclimb-test");
  source.setUser("freeclimb");
  source.setPassword("testpassword");
  source.setMaxConnections(10);

  private val flyway = new Flyway()
  flyway.setDataSource(source)
  flyway.migrate()

  def newSession[I <: IsolationLevel](isolationLevel: I) = new DbSession[I] {
    override val dbConnection = source.getConnection()
    override val level = isolationLevel
  }

  def newSession(): DbSession[TransactionReadCommitted] = newSession(TransactionReadCommitted)
}
