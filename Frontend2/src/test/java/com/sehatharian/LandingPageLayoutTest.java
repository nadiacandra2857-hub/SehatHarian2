package com.sehatharian;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pengujian Validasi FXML Landing Page Tampilan Baru")
class LandingPageLayoutTest {

    @BeforeAll
    static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already started
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Memuat FXML LandingPageView dan mengonfirmasi semua komponen FXML terinjeksi")
    void testLandingPageViewLoad() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LandingPageView.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Root LandingPageView tidak boleh null");
                assertNotNull(loader.getController(), "Controller LandingPageController tidak boleh null");
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            error[0].printStackTrace();
            throw new RuntimeException(error[0]);
        }
    }
}
