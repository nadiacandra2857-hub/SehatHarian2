package com.sehatharian;

import com.sehatharian.config.DatabaseBootstrap;
import com.sehatharian.config.LocalDatabase;
import com.sehatharian.context.ApplicationContext;
import com.sehatharian.repository.PenggunaRepository;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {
    private ApplicationContext context;
    private com.jpro.webapi.WebAPI browserApi;
    private com.jpro.webapi.InstanceCloseListener browserCloseListener;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Inisialisasi koordinator navigasi desktop
            PenggunaRepository accounts = new PenggunaRepository();
            context = new ApplicationContext(accounts, accounts);
            context.navigator().attachStage(primaryStage);

            try {
                var iconUrl = getClass().getResource("/images/logo.png");
                if (iconUrl != null) {
                    primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
                }
            } catch (Exception ignored) {}

            primaryStage.setMinWidth(360);
            primaryStage.setMinHeight(480);

            // Bersihkan thread background saat aplikasi ditutup
            primaryStage.setOnCloseRequest(event -> {
                closeContext();
                System.out.println("Instance SehatHarian ditutup.");
            });

            // Mulai di Halaman Landing Page (Sesuai Alur Spesifikasi Tahap 4 & 6)
            context.navigator().showLandingPage();
            primaryStage.show();
            if (com.jpro.webapi.WebAPI.isBrowser()) {
                browserApi = com.jpro.webapi.WebAPI.getWebAPI(primaryStage);
                browserCloseListener = this::closeContext;
                // Exact SDK hook: terminal instance closure, not transient websocket disconnect.
                browserApi.addInstanceCloseListener(browserCloseListener);
            }

        } catch (Exception e) {
            closeContext();
            e.printStackTrace();
            System.err.println("Gagal memulai antarmuka JavaFX: " + e.getMessage());
        }
    }

    private void closeContext() {
        if (browserApi != null && browserCloseListener != null) {
            browserApi.removeInstanceCloseListener(browserCloseListener);
            browserCloseListener = null;
        }
        if (context != null) context.close();
    }
    @Override public void stop() { closeContext(); }

    public static void main(String[] args) throws Exception {
        LocalDatabase.startIfNeeded();
        DatabaseBootstrap.initialize();
        launch(args);
    }
}
