package com.sehatharian;

import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only; never reads db.properties, starts a server, seeds or migrates application data. */
@EnabledIfEnvironmentVariable(named = "SEHATHARIAN_JDBC_TEST_URL", matches = "jdbc:mysql:.*")
class JdbcTransactionIntegrationTest {
    private Connection open() throws SQLException {
        var connection = DriverManager.getConnection(System.getenv("SEHATHARIAN_JDBC_TEST_URL"),
            System.getenv().getOrDefault("SEHATHARIAN_JDBC_TEST_USER", ""),
            System.getenv().getOrDefault("SEHATHARIAN_JDBC_TEST_PASSWORD", ""));
        try {
            // Check the selected schema on every connection before issuing any DDL/DML.
            if (connection.getCatalog() == null || !connection.getCatalog().matches("sehatharian_test_[a-zA-Z0-9_]+"))
                throw new SQLException("Integration test requires a dedicated sehatharian_test_* schema.");
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    @Test void innodbCommitAndRollbackAreVisibleThroughIndependentConnections() throws Exception {
        String table = "p1c_probe_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection setup = open(); Statement ddl = setup.createStatement()) {
            var metadata = setup.getMetaData();
            assertTrue(metadata.supportsTransactions());
            System.out.printf("JDBC probe: %s %s; %s %s; isolation=%d%n",
                metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion(),
                metadata.getDriverName(), metadata.getDriverVersion(), setup.getTransactionIsolation());
            ddl.executeUpdate("CREATE TABLE " + table + " (id INT PRIMARY KEY) ENGINE=InnoDB");
            try {
                try (PreparedStatement engine = setup.prepareStatement(
                        "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
                    engine.setString(1, table);
                    try (ResultSet rs = engine.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("InnoDB", rs.getString(1));
                    }
                }
                try (var tx = WriteTransaction.accountCreation(this::open)) {
                    Connection connection = tx.open();
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " VALUES (1)")) {
                        assertEquals(1, tx.execute(insert));
                        assertEquals(0, count(table));
                        assertTrue(tx.confirm(true));
                    }
                }
                assertEquals(1, count(table));
                try (var tx = WriteTransaction.accountCreation(this::open)) {
                    Connection connection = tx.open();
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " VALUES (2)")) {
                        assertEquals(1, tx.execute(insert));
                        tx.abort();
                        assertNull(connection.getWarnings());
                    }
                }
                assertEquals(1, count(table));
            } finally {
                ddl.executeUpdate("DROP TABLE " + table);
            }
        }
    }

    private int count(String table) throws SQLException {
        try (Connection observer = open(); Statement query = observer.createStatement();
             ResultSet rows = query.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }
}
