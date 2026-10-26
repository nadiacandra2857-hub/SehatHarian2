package com.sehatharian;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pengujian Validasi Seluruh FXML dan Inisialisasi Controller")
class AllFxmlViewsLoadTest {

    @BeforeAll
    static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // JavaFX Platform already initialized
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/fxml/LandingPageView.fxml",
        "/fxml/LoginView.fxml",
        "/fxml/CreateAccountView.fxml",
        "/fxml/OnboardingView.fxml",
        "/fxml/MainLayout.fxml",
        "/fxml/DashboardView.fxml",
        "/fxml/TantanganView.fxml",
        "/fxml/CatatanKebiasaanView.fxml",
        "/fxml/JadwalTidurView.fxml",
        "/fxml/StatistikMingguanView.fxml",
        "/fxml/EvaluasiMingguanView.fxml",
        "/fxml/ProfilPengaturanView.fxml",
        "/fxml/RekomendasiView.fxml"
    })
    @DisplayName("Memuat FXML dan memeriksa tidak ada error parsing atau binding controller")
    void testFxmlLoad(String fxmlPath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                var url = getClass().getResource(fxmlPath);
                assertNotNull(url, "File FXML harus ditemukan: " + fxmlPath);
                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();
                assertNotNull(root, "Root node tidak boleh null untuk: " + fxmlPath);
                assertNotNull(loader.getController(), "Controller tidak boleh null untuk: " + fxmlPath);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Loading FXML timeout untuk: " + fxmlPath);
        if (error[0] != null) {
            error[0].printStackTrace();
            throw new RuntimeException("Error memuat FXML: " + fxmlPath, error[0]);
        }
    }
}
