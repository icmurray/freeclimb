package freeclimb

import com.googlecode.flyway.core.Flyway

import javax.sql.DataSource

import org.postgresql.ds._

package object sql {
  def createConnectionPool(
      server: String,
      database: String,
      user: String,
      password: String,
      maxConnections: Int = 10) = {

    val source = new PGPoolingDataSource();
    source.setDataSourceName("PGPooling Datasource")
    source.setServerName(server);
    source.setDatabaseName(database);
    source.setUser(user);
    source.setPassword(password);
    source.setMaxConnections(maxConnections);
    source
  }

  def performMigrations(source: DataSource) {
    val flyway = new Flyway()
    flyway.setDataSource(source)
    flyway.migrate()
  }
}
