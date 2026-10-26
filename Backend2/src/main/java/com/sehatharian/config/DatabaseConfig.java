package com.sehatharian.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConfig mengelola konfigurasi dan koneksi JDBC ke MySQL.
 * Mendukung pembacaan berkas eksternal db.properties, connection timeout,
 * dan fail-safe check agar UI desktop tidak mengalami freezing jika database offline.
 */
public class DatabaseConfig {
    private static final Properties props = new Properties();
    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static boolean isInitialized = false;

    static {
        loadConfiguration();
    }

    public static synchronized void loadConfiguration() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Peringatan: Gagal membaca db.properties, menggunakan konfigurasi default: " + e.getMessage());
        }

        String host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : props.getProperty("db.host", "localhost");
        String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : props.getProperty("db.port", "3306");
        String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : props.getProperty("db.name", "sehatharian_db");
        username = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : props.getProperty("db.user", "root");
        password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : props.getProperty("db.password", "");
        String useSSL = props.getProperty("db.useSSL", "false");
        String allowPublicKey = props.getProperty("db.allowPublicKeyRetrieval", "true");
        String timezone = props.getProperty("db.serverTimezone", "Asia/Jakarta");

        // Timeout pendek (1.5 detik) agar operasi UI cepat fallback jika MySQL mati
        jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s&connectTimeout=1500&socketTimeout=2000",
            host, port, dbName, useSSL, allowPublicKey, timezone
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            isInitialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL Connector/J tidak ditemukan: " + e.getMessage());
        }
    }

    private static volatile Boolean dbAvailable = null;
    private static volatile long lastCheckTime = 0;
    private static final long CHECK_COOLDOWN_MS = 5000; // 5 detik cache status

    public static boolean isDatabaseAvailable() {
        long now = System.currentTimeMillis();
        if (dbAvailable != null && (now - lastCheckTime < CHECK_COOLDOWN_MS)) {
            return dbAvailable;
        }
        return testConnection();
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            loadConfiguration();
        }

        long now = System.currentTimeMillis();
        if (Boolean.FALSE.equals(dbAvailable) && (now - lastCheckTime < CHECK_COOLDOWN_MS)) {
            throw new SQLException("MySQL Server lokal sedang offline (mode in-memory demo aktif).");
        }

        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            dbAvailable = true;
            lastCheckTime = now;
            return conn;
        } catch (SQLException e) {
            dbAvailable = false;
            lastCheckTime = now;
            throw e;
        }
    }

    public static boolean testConnection() {
        if (!isInitialized) {
            loadConfiguration();
        }
        long now = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            dbAvailable = (conn != null && !conn.isClosed());
            lastCheckTime = now;
            return dbAvailable;
        } catch (SQLException e) {
            dbAvailable = false;
            lastCheckTime = now;
            return false;
        }
    }

    public static String getJdbcUrl() {
        return jdbcUrl;
    }

    public static String getUsername() {
        return username;
    }
}
