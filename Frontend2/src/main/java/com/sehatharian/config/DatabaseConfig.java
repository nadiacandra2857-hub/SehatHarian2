package com.sehatharian.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties props = new Properties();
    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static boolean isInitialized = false;

    static {
        loadConfiguration();
    }

    private static synchronized void loadConfiguration() {
        if (isInitialized) return;

        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Peringatan: Gagal membaca db.properties, menggunakan konfigurasi default: " + e.getMessage());
        }

        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "3306");
        String dbName = props.getProperty("db.name", "sehatharian_db");
        username = props.getProperty("db.user", "root");
        password = props.getProperty("db.password", "");
        String useSSL = props.getProperty("db.useSSL", "false");
        String allowPublicKey = props.getProperty("db.allowPublicKeyRetrieval", "true");
        String timezone = props.getProperty("db.serverTimezone", "Asia/Jakarta");

        jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s",
            host, port, dbName, useSSL, allowPublicKey, timezone
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            isInitialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL Connector/J tidak ditemukan: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            loadConfiguration();
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Koneksi MySQL gagal: " + e.getMessage());
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
