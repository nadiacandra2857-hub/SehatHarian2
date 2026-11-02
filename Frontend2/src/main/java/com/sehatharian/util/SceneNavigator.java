package com.sehatharian.util;

import com.sehatharian.model.UserSession;
import com.sehatharian.service.AuthService;
import com.sehatharian.service.PenggunaService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

/** Navigation/Stage and page cache belong exclusively to one application instance. */
public final class SceneNavigator {
    public enum Page {
        LANDING("LandingPageView.fxml", false, "SehatHarian — Membangun Rutinitas Sehat Harian", 1240, 800),
        LOGIN("LoginView.fxml", false, "Masuk ke Akun — SehatHarian", 1080, 750),
        REGISTER("CreateAccountView.fxml", false, "Buat Akun Baru — SehatHarian", 1080, 780),
        ONBOARDING("OnboardingView.fxml", true, "Informasi Dasar Pengguna — SehatHarian", 1120, 760),
        MAIN("MainLayout.fxml", true, "SehatHarian — Platform Tantangan Kesehatan Harian Adaptif", 1240, 800),
        DASHBOARD("DashboardView.fxml", true, "Dashboard", 1240, 800),
        TANTANGAN("TantanganView.fxml", true, "Tantangan", 1240, 800),
        REKOMENDASI("RekomendasiView.fxml", true, "Rekomendasi", 1240, 800),
        KEBIASAAN("CatatanKebiasaanView.fxml", true, "Catatan", 1240, 800),
        TIDUR("JadwalTidurView.fxml", true, "Tidur", 1240, 800),
        STATISTIK("StatistikMingguanView.fxml", true, "Statistik", 1240, 800),
        EVALUASI("EvaluasiMingguanView.fxml", true, "Evaluasi", 1240, 800),
        PROFIL("ProfilPengaturanView.fxml", true, "Profil", 1240, 800);

        private final String file;
        private final boolean privatePage;
        private final String title;
        private final double width, height;
        Page(String file, boolean privatePage, String title, double width, double height) {
            this.file = file; this.privatePage = privatePage; this.title = title;
            this.width = width; this.height = height;
        }
        public String path() { return "/fxml/" + file; }
        public boolean isPrivate() { return privatePage; }
        public static Page fromPath(String path) {
            for (Page page : values()) if (page.path().equals(path)) return page;
            throw new IllegalArgumentException("Halaman tidak dikenal.");
        }
    }

    public record LoadedView(Parent root, Object controller) implements AutoCloseable {
        public LoadedView { Objects.requireNonNull(root); Objects.requireNonNull(controller); }
        @Override public void close() {
            releaseController(controller);
        }
    }
    private final UserSession session;
    private final AuthService authService;
    private final InstanceControllerFactory controllers;
    private final PrivatePageCache<LoadedView> privatePages;
    private Stage primaryStage;
    private LoadedView currentView;
    private final com.sehatharian.service.PengingatTidurService reminders;
    private final SoundEffects sounds;
    private final Runnable detachIdentity;
    private boolean disposed;
    private Page currentPage = Page.LANDING;

    public SceneNavigator(UserSession session, AuthService authService, PenggunaService profiles,
            com.sehatharian.service.PengingatTidurService reminders, SoundEffects sounds) {
        this.session = Objects.requireNonNull(session);
        this.authService = Objects.requireNonNull(authService);
        this.reminders = Objects.requireNonNull(reminders);
        this.sounds = Objects.requireNonNull(sounds);
        this.privatePages = new PrivatePageCache<>(session);
        this.controllers = new InstanceControllerFactory(session, authService, profiles, this, reminders, sounds);
        detachIdentity = session.onIdentityChanged(() -> {
            currentPage = Page.LANDING;
            releaseCurrentView();
        });
    }

    /** Called once by MainApp.start; never replaces another instance's Stage. */
    public void attachStage(Stage stage) {
        ensureOpen();
        Objects.requireNonNull(stage);
        if (primaryStage != null && primaryStage != stage) throw new IllegalStateException("Stage sudah terpasang.");
        primaryStage = stage;
    }
    public Stage getPrimaryStage() { return primaryStage; }
    public Page currentPage() { return currentPage; }
    public int cachedPrivatePageCount() { return privatePages.size(); }

    /** The same decision is used by real navigation and can be checked without a toolkit. */
    public Page resolveDestination(Page requested) {
        ensureOpen();
        Objects.requireNonNull(requested);
        return requested.isPrivate() && !session.isLoggedIn() ? Page.LOGIN : requested;
    }

    public FXMLLoader newLoader(String path) {
        ensureOpen();
        Page page = Page.fromPath(path);
        if (page.isPrivate()) session.requireActiveUser();
        var resource = Objects.requireNonNull(SceneNavigator.class.getResource(path), "FXML tidak tersedia.");
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(controllers);
        return loader;
    }

    public LoadedView loadPrivateView(String path) throws IOException {
        ensureOpen();
        Page page = Page.fromPath(path);
        if (!page.isPrivate()) throw new IllegalArgumentException("Cache hanya untuk halaman privat.");
        LoadedView cached = privatePages.get(path);
        if (cached != null) return cached;
        long generation = session.getGeneration();
        FXMLLoader loader = newLoader(path);
        LoadedView loaded = loadView(loader);
        if (!session.isLoggedIn() || generation != session.getGeneration()) {
            loaded.close();
            throw new SecurityException("Hasil pemuatan halaman berasal dari sesi lama.");
        }
        privatePages.put(path, loaded);
        return loaded;
    }

    private static void releaseController(Object controller) {
        if (controller instanceof AutoCloseable resource) {
            try { resource.close(); } catch (Exception e) { System.err.println("Resource view gagal dilepas."); }
        }
    }
    private LoadedView loadView(FXMLLoader loader) throws IOException {
        try { Parent root = loader.load(); PageDesign.install(root); return new LoadedView(root, loader.getController()); }
        catch (IOException | RuntimeException e) {
            releaseController(loader.getController());
            throw e;
        }
    }

    public void showLandingPage() { show(Page.LANDING); }
    public void showLogin() { show(Page.LOGIN); }
    public void showCreateAccount() { show(Page.REGISTER); }
    public void showOnboarding() { show(Page.ONBOARDING); }
    public void showMainApp() { show(Page.MAIN); }

    public void logout() {
        if (disposed) return;
        authService.logout(); // Invalidates only this session and its private page cache.
        currentPage = Page.LANDING;
        // Logical logout also works before a Stage is attached (e.g. an isolated context test).
        if (primaryStage != null) {
            // Do not leave a private root visible if loading the public page fails.
            clearStage(primaryStage);
            showLandingPage();
        }
    }

    /** Keep a Scene attached while removing private content; JPro tracks scene properties during logout/close. */
    private static void clearStage(Stage stage) {
        if (stage.getScene() != null) stage.getScene().setRoot(new javafx.scene.layout.StackPane());
    }

    private void releaseCurrentView() {
        privatePages.clear();
        if (currentView != null) { currentView.close(); currentView = null; }
    }
    private void ensureOpen() {
        if (disposed) throw new IllegalStateException("Navigator sudah ditutup.");
    }
    /** Terminal per-instance cleanup, also used by the context and actual close hooks. */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        reminders.close();
        sounds.close();
        authService.logout();
        releaseCurrentView();
        privatePages.close();
        detachIdentity.run();
        Stage detached = primaryStage;
        primaryStage = null;
        if (detached != null) {
            if (javafx.application.Platform.isFxApplicationThread()) clearStage(detached);
            else {
                try { javafx.application.Platform.runLater(() -> detached.setScene(null)); }
                catch (IllegalStateException e) { /* Toolkit already stopped; resources above are closed. */ }
            }
        }
    }

    private void show(Page requested) {
        Page destination = resolveDestination(requested);
        if (primaryStage == null) throw new IllegalStateException("Stage instance belum dipasang.");
        try {
            long generation = session.getGeneration();
            releaseCurrentView();
            FXMLLoader loader = newLoader(destination.path());
            LoadedView nextView = loadView(loader);
            Parent root = nextView.root();
            if (destination.isPrivate() && (!session.isLoggedIn() || generation != session.getGeneration())) {
                nextView.close();
                throw new SecurityException("Hasil navigasi berasal dari sesi lama.");
            }
            currentView = nextView;
            LayoutGuard.protect(root);
            Scene scene = primaryStage.getScene();
            if (scene == null) primaryStage.setScene(new Scene(root, destination.width, destination.height));
            else scene.setRoot(root);
            primaryStage.setTitle(destination.title);
            primaryStage.setMinWidth(360);
            primaryStage.setMinHeight(480);
            currentPage = destination;
            AnimationHelper.fadeIn(root, 300);
        } catch (IOException e) {
            releaseCurrentView();
            if (primaryStage != null) clearStage(primaryStage);
            throw new IllegalStateException("Gagal memuat halaman aplikasi.", e);
        } catch (RuntimeException e) {
            releaseCurrentView();
            if (primaryStage != null) clearStage(primaryStage);
            throw e;
        }
    }
}
