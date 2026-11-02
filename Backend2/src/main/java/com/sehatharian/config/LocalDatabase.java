package com.sehatharian.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Owns the local database process for the lifetime of the desktop JVM. */
public final class LocalDatabase {
    private static Process process;

    private LocalDatabase() {}

    public static synchronized void startIfNeeded() throws IOException {
        String url = DatabaseConfig.getJdbcUrl();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") || !url.startsWith("jdbc:mysql://127.0.0.1:13306/")
                || DatabaseConfig.testConnection()) {
            return;
        }
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("scripts/with-database.sh"))) {
            root = root.getParent();
        }
        if (root == null) {
            throw new IOException("Peluncur database tidak ditemukan. Jalankan aplikasi dari folder proyek.");
        }
        process = new ProcessBuilder("bash", root.resolve("scripts/with-database.sh").toString())
                .directory(root.toFile()).redirectErrorStream(true).start();
        Runtime.getRuntime().addShutdownHook(new Thread(LocalDatabase::stop, "database-shutdown"));
        BufferedReader output = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = output.readLine()) != null) {
            if (line.equals("SEHATHARIAN_DATABASE_READY")) return;
            System.out.println(line);
        }
        stop();
        throw new IOException("MariaDB lokal gagal menyala. Periksa .runtime/database/server.log.");
    }

    private static synchronized void stop() {
        if (process == null) return;
        try {
            process.getOutputStream().close();
            if (!process.waitFor(15, TimeUnit.SECONDS)) process.destroy();
        } catch (IOException e) {
            process.destroy();
        } catch (InterruptedException e) {
            process.destroy();
            Thread.currentThread().interrupt();
        }
        process = null;
    }
}
