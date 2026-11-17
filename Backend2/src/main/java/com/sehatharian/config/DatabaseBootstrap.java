package com.sehatharian.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates missing schema only; never inserts accounts, personal records or curated content. */
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
            execute(statement, read("rombak.sql"));
            execute(statement, read("training-history.sql"));
            execute(statement, read("training-checkpoint.sql"));
            execute(statement, read("personal-menu.sql"));
            execute(statement, read("curated-menu.sql"));
            execute(statement, read("sleep-completion.sql"));
            // Existing installations need the new column too; never promote existing users.
            boolean hasRole;
            try (var columns = conn.getMetaData().getColumns(name, null, "pengguna", "role")) {
                hasRole = columns.next();
            }
            if (!hasRole) statement.executeUpdate(
                    "ALTER TABLE pengguna ADD COLUMN role ENUM('user','admin') NOT NULL DEFAULT 'user'");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_setup (id INT PRIMARY KEY)");
            // Provision schema only. Existing accounts/content remain untouched; demo fixtures are opt-in tests.
            statement.executeUpdate("INSERT IGNORE INTO app_setup (id) VALUES (1)");
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
