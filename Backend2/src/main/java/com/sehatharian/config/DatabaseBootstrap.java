package com.sehatharian.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

/** Creates missing tables from the bundled SQL without reseeding existing user data. */
public final class DatabaseBootstrap {
    private DatabaseBootstrap() {}

    public static void initialize() throws SQLException, IOException {
        String name = DatabaseConfig.getDatabaseName();
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Nama database hanya boleh berisi huruf, angka, dan underscore.");
        }
        String url = DatabaseConfig.getJdbcUrl();
        int queryStart = url.indexOf('?');
        String databaseUrl = url.substring(0, queryStart);
        String serverUrl = databaseUrl.substring(0, databaseUrl.lastIndexOf('/') + 1)
                + url.substring(queryStart);
        try (Connection conn = DriverManager.getConnection(serverUrl,
                DatabaseConfig.getUsername(), DatabaseConfig.getPassword());
             Statement statement = conn.createStatement()) {
            try (var lock = statement.executeQuery("SELECT GET_LOCK('sehatharian_setup_" + name + "', 30)")) {
                if (!lock.next() || lock.getInt(1) != 1) {
                    throw new SQLException("Inisialisasi database sedang berjalan. Coba kembali.");
                }
            }
            execute(statement, read("schema.sql").replace("sehatharian_db", name));
            // Existing installations need the new column too; never promote existing users.
            boolean hasRole;
            try (var columns = conn.getMetaData().getColumns(name, null, "pengguna", "role")) {
                hasRole = columns.next();
            }
            if (!hasRole) statement.executeUpdate(
                    "ALTER TABLE pengguna ADD COLUMN role ENUM('user','admin') NOT NULL DEFAULT 'user'");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_setup (id INT PRIMARY KEY)");
            boolean initialized;
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM app_setup WHERE id = 1")) {
                result.next();
                initialized = result.getInt(1) > 0;
            }
            if (!initialized) {
                boolean empty = true;
                for (String table : new String[]{"pengguna", "tantangan", "rekomendasi"}) {
                    try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        result.next();
                        empty &= result.getLong(1) == 0;
                    }
                }
                // Seed uses fixed IDs: only apply it to a newly created, empty database.
                conn.setAutoCommit(false);
                try {
                    if (empty) {
                        String seed = read("seed.sql").replace("USE sehatharian_db;", "USE " + name + ";");
                        seed = seed.replace("$2a$10$wG0.1d80gM63wZ4tKzO90.7nUq2D/jI485n6bH4x79mK1s6U5mX8q",
                                BCrypt.hashpw("password123", BCrypt.gensalt()));
                        execute(statement, seed);
                    }
                    statement.executeUpdate("INSERT INTO app_setup (id) VALUES (1)");
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
        if (!DatabaseConfig.testConnection()) {
            throw new SQLException("Database selesai disiapkan tetapi koneksi aplikasi gagal.");
        }
        System.out.println("Database SehatHarian siap; data tersimpan permanen.");
    }

    private static String read(String resource) throws IOException {
        try (var stream = DatabaseBootstrap.class.getResourceAsStream("/" + resource)) {
            if (stream == null) throw new IOException("SQL tidak ditemukan: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void execute(Statement statement, String sql) throws SQLException {
        // Canonical project scripts contain no procedures or quoted semicolons.
        String cleaned = sql.replaceAll("(?m)--[^\\r\\n]*", "");
        for (String command : cleaned.split(";")) {
            if (!command.isBlank()) statement.execute(command.trim());
        }
    }
}
