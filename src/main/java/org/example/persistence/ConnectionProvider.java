package org.example.persistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Hands out database connections to the DAOs. The DAOs don't care where a connection comes from:
// the console opens a new one each time (DriverManager), the web application borrows one from
// Spring's connection pool (HikariCP). Closing a pooled connection gives it back to the pool.
public class ConnectionProvider {

    @FunctionalInterface
    private interface ConnectionSource {
        Connection open() throws SQLException;
    }

    private final ConnectionSource source;

    // Console: a fresh connection with the settings from db.properties
    public ConnectionProvider(DatabaseConfig config) {
        this.source = () -> DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    // Web application: connections from the pool Spring Boot creates
    public ConnectionProvider(DataSource dataSource) {
        this.source = dataSource::getConnection;
    }

    public Connection getConnection() throws SQLException {
        return source.open();
    }
}
