package com.sehatharian;

import javafx.application.Platform;
import com.sehatharian.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
    private ApplicationContext context;

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

    @BeforeEach void clearSession() { context = FrontendAuthFixture.context(); }
    @AfterEach void cleanSession() { context.session().logout(); }

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
    @DisplayName("Halaman publik dimuat; halaman pribadi menolak sesi anonim")
    void testFxmlLoad(String fxmlPath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                var url = getClass().getResource(fxmlPath);
                assertNotNull(url, "File FXML harus ditemukan: " + fxmlPath);
                FXMLLoader loader = context.navigator().newLoader(fxmlPath);
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
        boolean protectedPage = !java.util.Set.of("/fxml/LandingPageView.fxml", "/fxml/LoginView.fxml",
                "/fxml/CreateAccountView.fxml").contains(fxmlPath);
        if (protectedPage) {
            Throwable cause = error[0];
            while (cause != null && !(cause instanceof SecurityException)) cause = cause.getCause();
            assertTrue(cause instanceof SecurityException, "Protected FXML must reject an anonymous identity");
        } else if (error[0] != null) {
            throw new RuntimeException("Error memuat FXML: " + fxmlPath, error[0]);
        }
    }
}
