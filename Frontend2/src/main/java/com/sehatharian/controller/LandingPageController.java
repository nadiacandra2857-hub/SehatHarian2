package com.sehatharian.controller;

import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DialogHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

public class LandingPageController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox contentVBox;

    // Hero Section
    @FXML private StackPane heroStackPane;
    @FXML private Region heroAnimatedRegion;
    @FXML private VBox heroTextContainer;
    @FXML private ImageView navbarLogoView;

    // Navigation buttons
    @FXML private Button btnNavDeskripsi;
    @FXML private Button btnNavFitur;
    @FXML private Button btnNavDownloads;
    @FXML private Button btnNavBantuan;
    @FXML private Button btnNavLogin;
    @FXML private Button btnNavCreateAccount;

    // Hero buttons
    @FXML private Button btnHeroLogin;
    @FXML private Button btnHeroCreateAccount;

    // Showcase Section ("Foto 1")
    @FXML private VBox deskripsiSectionBox;
    @FXML private ImageView foto1ImageView;

    // Gradient Transition Bands
    @FXML private Region gradientBand1;
    @FXML private Region gradientBand2;
    @FXML private StackPane downloadSectionBox;

    // Features Section ("Foto 2, 3, 4, 5")
    @FXML private VBox fiturSectionBox;
    @FXML private VBox cardFitur1;
    @FXML private VBox cardFitur2;
    @FXML private VBox cardFitur3;
    @FXML private VBox cardFitur4;
    @FXML private ImageView foto2ImageView;
    @FXML private ImageView foto3ImageView;
    @FXML private ImageView foto4ImageView;
    @FXML private ImageView foto5ImageView;

    // Footer
    @FXML private VBox footerSectionBox;
    @FXML private ImageView footerLogoView;

    // Gradient Animation
    private Timeline gradientTimeline;
    private final DoubleProperty gradientPhase = new SimpleDoubleProperty(0.0);

    @FXML
    public void initialize() {
        muatGambar();
        setupHeroBackgroundSizing();
        setupAnimatedGradientBackground();
        setupKeyNavigation();
        setupEntranceAnimations();
    }

    /**
     * Mengatur ukuran Region gradasi animasi agar mengisi penuh StackPane.
     */
    private void setupHeroBackgroundSizing() {
        if (heroAnimatedRegion != null && heroStackPane != null) {
            heroAnimatedRegion.setManaged(false);
            heroAnimatedRegion.setMouseTransparent(true);
            heroAnimatedRegion.prefWidthProperty().bind(heroStackPane.widthProperty());
            heroAnimatedRegion.prefHeightProperty().bind(heroStackPane.heightProperty());
        }
        if (gradientBand2 != null && downloadSectionBox != null) {
            gradientBand2.setManaged(false);
            gradientBand2.setMouseTransparent(true);
            gradientBand2.prefWidthProperty().bind(downloadSectionBox.widthProperty());
            gradientBand2.prefHeightProperty().bind(downloadSectionBox.heightProperty());
        }
    }

    /**
     * Menjalankan animasi gradasi halus yang terus bergerak di background:
     * Biru Tua (#0009B9) -> Biru Muda (#407EBD) -> Hijau Mint (#82F5C1).
     */
    private void setupAnimatedGradientBackground() {
        gradientTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(gradientPhase, 0.0)),
            new KeyFrame(Duration.seconds(8), new KeyValue(gradientPhase, 1.0, Interpolator.EASE_BOTH))
        );
        gradientTimeline.setAutoReverse(true);
        gradientTimeline.setCycleCount(Animation.INDEFINITE);

        gradientPhase.addListener((obs, oldVal, newVal) -> {
            double p = newVal.doubleValue();
            updateGradientBackground(p);
        });

        // Set initial frame
        updateGradientBackground(0.0);
        gradientTimeline.play();
    }

    /**
     * Memperbarui LinearGradient pada background berdasarkan fase animasi p (0.0 .. 1.0).
     */
    private void updateGradientBackground(double p) {
        // Vektor gradasi berosilasi lembut dari diagonal kiri-bawah ke kanan-atas
        double startX = 0.05 + 0.12 * p;
        double startY = 0.95 - 0.15 * (1.0 - p);
        double endX   = 0.88 + 0.12 * (1.0 - p);
        double endY   = 0.05 + 0.15 * p;

        double mid1 = 0.35 + 0.15 * p;
        double mid2 = 0.72 + 0.15 * (1.0 - p);

        Stop[] stops = new Stop[] {
            new Stop(0.0, Color.web("#0009B9")),
            new Stop(Math.min(0.60, Math.max(0.20, mid1)), Color.web("#407EBD")),
            new Stop(Math.min(0.95, Math.max(0.65, mid2)), Color.web("#82F5C1")),
            new Stop(1.0, Color.web("#82F5C1"))
        };

        LinearGradient gradient = new LinearGradient(
            startX, startY, endX, endY, true, CycleMethod.NO_CYCLE, stops
        );

        Background bg = new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY));

        if (heroAnimatedRegion != null) {
            heroAnimatedRegion.setBackground(bg);
        }
        if (gradientBand1 != null) {
            gradientBand1.setBackground(bg);
        }
        if (gradientBand2 != null) {
            gradientBand2.setBackground(bg);
        }
    }

    /**
     * Memuat aset gambar Foto 1, Foto 2, Foto 3, Foto 4, Foto 5, dan logo SehatHarian.
     */
    private void muatGambar() {
        try {
            setSafeImage(foto1ImageView, "/images/foto1_showcase.png");
            setSafeImage(foto2ImageView, "/images/foto2_tantangan.png");
            setSafeImage(foto3ImageView, "/images/foto3_hidrasi.png");
            setSafeImage(foto4ImageView, "/images/foto4_isipiringku.png");
            setSafeImage(foto5ImageView, "/images/foto5_tidur.png");

            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                Image logo = new Image(logoUrl.toExternalForm());
                if (navbarLogoView != null) navbarLogoView.setImage(logo);
                if (footerLogoView != null) footerLogoView.setImage(logo);
            }
        } catch (Exception e) {
            System.err.println("Info muat gambar landing page: " + e.getMessage());
        }
    }

    private void setSafeImage(ImageView view, String resourcePath) {
        if (view == null) return;
        try {
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                view.setImage(new Image(url.toExternalForm()));
            }
        } catch (Exception ignored) {}
    }

    private void setupEntranceAnimations() {
        Platform.runLater(() -> {
            if (heroTextContainer != null) {
                AnimationHelper.gsapSlideInUp(heroTextContainer, 28, 650);
            }
            if (foto1ImageView != null) {
                AnimationHelper.gsapSlideInUp(foto1ImageView, 32, 750);
            }
        });
    }

    private void setupKeyNavigation() {
        if (mainScrollPane == null) return;

        mainScrollPane.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.PAGE_DOWN) {
                mainScrollPane.setVvalue(Math.min(1.0, mainScrollPane.getVvalue() + 0.12));
                event.consume();
            } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.PAGE_UP) {
                mainScrollPane.setVvalue(Math.max(0.0, mainScrollPane.getVvalue() - 0.12));
                event.consume();
            }
        });
    }

    // =========================================================================
    // NAVBAR & SCROLLING CONTROLS
    // =========================================================================
    @FXML
    public void handleNavBeranda() {
        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(0.0);
        }
    }

    @FXML
    public void handleNavDeskripsi() {
        scrollToNode(deskripsiSectionBox);
    }

    @FXML
    public void handleNavFitur() {
        scrollToNode(fiturSectionBox);
    }

    @FXML
    public void handleNavDownloads() {
        scrollToNode(downloadSectionBox);
    }

    @FXML
    public void handleNavBantuan() {
        scrollToNode(footerSectionBox);
    }

    private void scrollToNode(Node target) {
        if (mainScrollPane == null || target == null || contentVBox == null) return;

        Platform.runLater(() -> {
            double targetY = target.getBoundsInParent().getMinY();
            double contentHeight = contentVBox.getHeight();
            if (contentHeight <= 0) contentHeight = contentVBox.getBoundsInLocal().getHeight();
            double viewportHeight = mainScrollPane.getViewportBounds().getHeight();
            double scrollableHeight = contentHeight - viewportHeight;

            if (scrollableHeight > 0) {
                double vValue = Math.min(1.0, Math.max(0.0, targetY / scrollableHeight));
                mainScrollPane.setVvalue(vValue);
            }
        });
    }

    // =========================================================================
    // FITUR INTERAKTIF CLICKS
    // =========================================================================
    @FXML
    public void handleFeature1Click() {
        DialogHelper.showInfo("Tantangan Harian & Gerak Mikro",
                "Fitur ini memberikan rekomendasi aktivitas gerak mikro harian:\n\n" +
                "• 15-30 menit latihan peregangan leher, bahu, dan punggung saat bekerja.\n" +
                "• Jalan santai ringan untuk melancarkan sirkulasi darah tanpa gym.\n" +
                "• Tantangan mikro adaptif yang disesuaikan dengan waktu luang Anda.\n\n" +
                "Klik 'Buat Akun' untuk mulai mencatat tantangan harian Anda!");
    }

    @FXML
    public void handleFeature2Click() {
        DialogHelper.showInfo("Pelacakan Hidrasi Presisi",
                "Panduan Kemenkes RI menganjurkan asupan cairan tubuh terkalibrasi:\n\n" +
                "• Formula Kemenkes: Berat Badan × 35 ml per hari (rata-rata 2.000 ml / 8 gelas).\n" +
                "• Rekomendasi jadwal minum bertahap: Pagi, siang, sore, dan malam.\n" +
                "• Pemantauan visual sekali sentuh untuk mencatat setiap gelas air.");
    }

    @FXML
    public void handleFeature3Click() {
        DialogHelper.showInfo("Gizi Seimbang 'Isi Piringku'",
                "Panduan resmi gizi seimbang Kementerian Kesehatan RI:\n\n" +
                "• 1/3 piring: Sayur-sayuran (sumber serat & mikronutrien).\n" +
                "• 1/3 piring: Makanan Pokok (karbohidrat kompleks).\n" +
                "• 1/6 piring: Buah-buahan segar.\n" +
                "• 1/6 piring: Lauk pauk sumber protein hewani & nabati.");
    }

    @FXML
    public void handleFeature4Click() {
        DialogHelper.showInfo("Pemulihan Ritme Sirkadian",
                "Stabilitas ritme sirkadian untuk pemulihan optimal:\n\n" +
                "• Target tidur: 7 hingga 8 jam per malam dengan jadwal konsisten.\n" +
                "• Pengingat mematikan layar gawai 30 menit sebelum tidur.\n" +
                "• Evaluasi kualitas pemulihan dan Skor Sehat mingguan.");
    }

    // =========================================================================
    // AUTH & FOOTER ACTIONS
    // =========================================================================
    @FXML
    public void handleLogin() {
        SceneNavigator.showLogin();
    }

    @FXML
    public void handleCreateAccount() {
        SceneNavigator.showCreateAccount();
    }

    @FXML
    public void handleContactEmail() {
        DialogHelper.showInfo("Pusat Bantuan & Dukungan",
                "Tim Layanan Pengguna SehatHarian siap membantu Anda!\n\n" +
                "Email Resmi: bantuan@sehatharian.id\n" +
                "Jam Operasional: Senin - Jumat, 08.00 - 17.00 WIB\n\n" +
                "Kirimkan pertanyaan terkait fitur, panduan gizi harian, atau kendala akun.");
    }

    @FXML
    public void handleFooterPrivacy() {
        DialogHelper.showInfo("Kebijakan Privasi",
                "Keamanan Data Anda adalah Prioritas Kami:\n\n" +
                "• Catatan kesehatan harian, hidrasi, dan jadwal tidur tersimpan aman.\n" +
                "• SehatHarian tidak membagikan atau menjual data kebiasaan kepada pihak ketiga.");
    }

    @FXML
    public void handleFooterTerms() {
        DialogHelper.showInfo("Syarat & Ketentuan",
                "Pedoman Penggunaan Platform:\n\n" +
                "1. Seluruh fitur tantangan dan target dirancang untuk membangun rutinitas sehat mandiri.\n" +
                "2. Konten tidak dimaksudkan sebagai pengganti diagnosis, resep, atau perawatan dokter.");
    }
}