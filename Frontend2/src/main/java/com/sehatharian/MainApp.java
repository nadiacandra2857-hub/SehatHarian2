package com.sehatharian;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.service.PengingatTidurService;
import com.sehatharian.util.SceneNavigator;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Inisialisasi koordinator navigasi desktop
            SceneNavigator.init(primaryStage);

            // Verifikasi koneksi MySQL saat aplikasi dibuka (non-blocking)
            boolean dbConnected = DatabaseConfig.testConnection();
            if (!dbConnected) {
                System.out.println("Mode Demo Frontend Aktif: Tidak dapat terhubung ke MySQL Server lokal. State in-memory aktif.");
            }

            try {
                var iconUrl = getClass().getResource("/images/logo.png");
                if (iconUrl != null) {
                    primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
                }
            } catch (Exception ignored) {}

            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(680);

            // Bersihkan thread background saat aplikasi ditutup
            primaryStage.setOnCloseRequest(event -> {
                PengingatTidurService.getInstance().stop();
                System.out.println("Aplikasi SehatHarian ditutup dengan aman.");
            });

            // Mulai di Halaman Landing Page (Sesuai Alur Spesifikasi Tahap 4 & 6)
            SceneNavigator.showLandingPage();
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Gagal memulai antarmuka JavaFX: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
