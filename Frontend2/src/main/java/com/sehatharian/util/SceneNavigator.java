package com.sehatharian.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * SceneNavigator mengelola navigasi halaman tingkat atas aplikasi desktop SehatHarian:
 * Landing Page -> Login / Create Account -> Onboarding 3 Slide -> Dashboard Utama.
 */
public class SceneNavigator {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showLandingPage() {
        loadScene("/fxml/LandingPageView.fxml", "SehatHarian — Membangun Rutinitas Sehat Harian", 1240, 800);
    }

    public static void showLogin() {
        loadScene("/fxml/LoginView.fxml", "Masuk ke Akun — SehatHarian", 1080, 750);
    }

    public static void showCreateAccount() {
        loadScene("/fxml/CreateAccountView.fxml", "Buat Akun Baru — SehatHarian", 1080, 780);
    }

    public static void showOnboarding() {
        loadScene("/fxml/OnboardingView.fxml", "Informasi Dasar Pengguna — SehatHarian", 1120, 760);
    }

    public static void showMainApp() {
        loadScene("/fxml/MainLayout.fxml", "SehatHarian — Platform Tantangan Kesehatan Harian Adaptif", 1240, 800);
    }

    private static void loadScene(String fxmlPath, String title, double width, double height) {
        if (primaryStage == null) {
            System.err.println("SceneNavigator belum diinisialisasi dengan primaryStage!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, width, height);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            primaryStage.setTitle(title);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(680);

            AnimationHelper.fadeIn(root, 300);

        } catch (IOException e) {
            System.err.println("Gagal memuat scene FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
