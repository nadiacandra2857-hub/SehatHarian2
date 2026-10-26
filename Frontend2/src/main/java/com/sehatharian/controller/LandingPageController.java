package com.sehatharian.controller;

import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DialogHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LandingPageController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox contentVBox;

    @FXML private StackPane heroStackPane;
    @FXML private ImageView heroBgImageView;
    @FXML private javafx.scene.layout.Region heroOverlayRegion;
    @FXML private VBox heroTextContainer;
    @FXML private VBox heroPreviewCard;
    @FXML private ImageView navbarLogoView;
    @FXML private ImageView aboutImageView;
    @FXML private ImageView footerLogoView;

    @FXML private ImageView featureImg1;
    @FXML private ImageView featureImg2;
    @FXML private ImageView featureImg3;
    @FXML private ImageView featureImg4;
    @FXML private ImageView featureImg5;

    // Navigation buttons
    @FXML private Button btnNavBeranda;
    @FXML private Button btnNavCoba;
    @FXML private Button btnNavFitur;
    @FXML private Button btnNavTentang;
    @FXML private Button btnNavBantuan;
    @FXML private Button btnNavLogin;
    @FXML private Button btnNavCreateAccount;

    // Hero buttons
    @FXML private Button btnHeroLogin;
    @FXML private Button btnHeroCreateAccount;
    @FXML private Button btnHeroDemo;

    // Sections
    @FXML private HBox penilaianSectionBox;
    @FXML private VBox interactiveDemoSectionBox;
    @FXML private VBox deskripsiSectionBox;
    @FXML private VBox fiturSectionBox;
    @FXML private VBox faqSectionBox;
    @FXML private VBox downloadSectionBox;
    @FXML private VBox footerSectionBox;

    // Interactive Demo Checklist
    @FXML private CheckBox chkDemoTantangan1;
    @FXML private CheckBox chkDemoTantangan2;
    @FXML private CheckBox chkDemoTantangan3;
    @FXML private CheckBox chkDemoTantangan4;
    @FXML private ProgressBar demoProgressBar;
    @FXML private Label lblDemoProgressText;
    @FXML private Label lblDemoCelebration;

    // Interactive Calculator
    @FXML private Slider sliderBeratBadan;
    @FXML private Label lblBeratBadanVal;
    @FXML private Label lblKebutuhanAirVal;
    @FXML private Label lblGelasAirVal;
    @FXML private Label lblTargetGerakVal;
    @FXML private Label lblTargetTidurVal;

    // Feature Filter Tabs
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterHarian;
    @FXML private Button btnFilterNutrisi;
    @FXML private Button btnFilterLaporan;
    @FXML private VBox cardFitur1;
    @FXML private VBox cardFitur2;
    @FXML private VBox cardFitur3;
    @FXML private VBox cardFitur4;
    @FXML private VBox cardFitur5;
    @FXML private VBox cardFitur6;

    // FAQ Accordion
    @FXML private Label lblFaqArrow1;
    @FXML private Label lblFaqArrow2;
    @FXML private Label lblFaqArrow3;
    @FXML private Label lblFaqArrow4;
    @FXML private VBox faqContent1;
    @FXML private VBox faqContent2;
    @FXML private VBox faqContent3;
    @FXML private VBox faqContent4;

    // CTA
    @FXML private Button btnCtaCreateAccount;
    @FXML private Button btnCtaLogin;
    @FXML private Button btnDistribusiInfo;

    @FXML
    public void initialize() {
        muatGambar();
        setupHeroBackgroundSizing();
        setupInteractiveCalculators();
        setupInteractiveChecklist();
        setupKeyNavigation();
        setupGsapEntranceAnimations();
    }

    private void setupGsapEntranceAnimations() {
        Platform.runLater(() -> {
            if (heroTextContainer != null) {
                AnimationHelper.gsapSlideInUp(heroTextContainer, 24, 650);
            }
            if (heroPreviewCard != null) {
                AnimationHelper.gsapSlideInUp(heroPreviewCard, 32, 750);
            }
            if (penilaianSectionBox != null) {
                AnimationHelper.fadeIn(penilaianSectionBox, 600);
            }
        });
    }

    private void muatGambar() {
        try {
            var heroBgUrl = getClass().getResource("/images/gambar1.jpg");
            if (heroBgUrl != null && heroBgImageView != null) {
                heroBgImageView.setImage(new Image(heroBgUrl.toExternalForm(), 1920, 0, true, true));
            }

            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                if (navbarLogoView != null) navbarLogoView.setImage(new Image(logoUrl.toExternalForm()));
                if (footerLogoView != null) footerLogoView.setImage(new Image(logoUrl.toExternalForm()));
            }

            var aboutUrl = getClass().getResource("/images/hospital_card_framed.png");
            if (aboutUrl == null) {
                aboutUrl = getClass().getResource("/images/hero_wellness.png");
            }
            if (aboutUrl != null && aboutImageView != null) {
                aboutImageView.setImage(new Image(aboutUrl.toExternalForm()));
            }

            // Fitur images
            setSafeImage(featureImg1, "/images/illustration_walk.png");
            setSafeImage(featureImg2, "/images/illustration_water.png");
            setSafeImage(featureImg3, "/images/illustration_stretch.png");
            setSafeImage(featureImg4, "/images/illustration_sleep.png");
            setSafeImage(featureImg5, "/images/illustration_food.png");

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

    private void setupHeroBackgroundSizing() {
        if (heroBgImageView != null && heroStackPane != null) {
            heroBgImageView.setManaged(false);
            heroBgImageView.setMouseTransparent(true);
            heroBgImageView.fitWidthProperty().bind(heroStackPane.widthProperty());
            heroBgImageView.fitHeightProperty().bind(heroStackPane.heightProperty());
        }
        if (heroOverlayRegion != null && heroStackPane != null) {
            heroOverlayRegion.setManaged(false);
            heroOverlayRegion.setMouseTransparent(true);
            heroOverlayRegion.prefWidthProperty().bind(heroStackPane.widthProperty());
            heroOverlayRegion.prefHeightProperty().bind(heroStackPane.heightProperty());
        }
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
    // INTERAKTIF WIDGET 1: CHECKLIST TANTANGAN
    // =========================================================================
    private void setupInteractiveChecklist() {
        if (chkDemoTantangan1 != null) chkDemoTantangan1.setSelected(true);
        handleUpdateDemoTantangan();
    }

    @FXML
    public void handleUpdateDemoTantangan() {
        int count = 0;
        if (chkDemoTantangan1 != null && chkDemoTantangan1.isSelected()) count++;
        if (chkDemoTantangan2 != null && chkDemoTantangan2.isSelected()) count++;
        if (chkDemoTantangan3 != null && chkDemoTantangan3.isSelected()) count++;
        if (chkDemoTantangan4 != null && chkDemoTantangan4.isSelected()) count++;

        double progress = count / 4.0;
        int percent = (int) Math.round(progress * 100);

        if (demoProgressBar != null) {
            AnimationHelper.gsapProgressBar(demoProgressBar, progress, 400);
        }

        if (lblDemoProgressText != null) {
            lblDemoProgressText.setText(count + " dari 4 Selesai (" + percent + "%)");
        }

        if (lblDemoCelebration != null) {
            boolean allDone = (count == 4);
            if (allDone && !lblDemoCelebration.isVisible()) {
                lblDemoCelebration.setVisible(true);
                lblDemoCelebration.setManaged(true);
                AnimationHelper.gsapPulseSuccess(lblDemoCelebration);
            } else if (!allDone) {
                lblDemoCelebration.setVisible(false);
                lblDemoCelebration.setManaged(false);
            }
        }
    }

    @FXML
    public void handleResetDemoTantangan() {
        if (chkDemoTantangan1 != null) chkDemoTantangan1.setSelected(false);
        if (chkDemoTantangan2 != null) chkDemoTantangan2.setSelected(false);
        if (chkDemoTantangan3 != null) chkDemoTantangan3.setSelected(false);
        if (chkDemoTantangan4 != null) chkDemoTantangan4.setSelected(false);
        handleUpdateDemoTantangan();
    }

    // =========================================================================
    // INTERAKTIF WIDGET 2: KALKULATOR HIDRASI & AKTIVITAS REAL-TIME
    // =========================================================================
    private void setupInteractiveCalculators() {
        if (sliderBeratBadan == null) return;

        sliderBeratBadan.valueProperty().addListener((obs, oldVal, newVal) -> {
            int berat = newVal.intValue();
            if (lblBeratBadanVal != null) {
                lblBeratBadanVal.setText(berat + " kg");
            }

            // Rumus Kemenkes: Kebutuhan air = BB * 35 ml
            int airMl = berat * 35;
            double gelas = airMl / 250.0;
            if (lblKebutuhanAirVal != null) {
                lblKebutuhanAirVal.setText(String.format("%,d ml", airMl).replace(',', '.'));
            }
            if (lblGelasAirVal != null) {
                lblGelasAirVal.setText(String.format("≈ %.1f Gelas/Hari", gelas));
            }

            // Target gerak & tidur adaptif
            if (lblTargetGerakVal != null) {
                int menit = (berat > 85) ? 25 : 30;
                lblTargetGerakVal.setText(menit + " Menit");
            }
            if (lblTargetTidurVal != null) {
                lblTargetTidurVal.setText("7.5 Jam");
            }
        });
    }

    // =========================================================================
    // INTERAKTIF WIDGET 3: FILTER TAB FITUR
    // =========================================================================
    @FXML
    public void handleFilterAll() {
        setActiveFilterTab(btnFilterAll);
        setCardHighlight(cardFitur1, true);
        setCardHighlight(cardFitur2, true);
        setCardHighlight(cardFitur3, true);
        setCardHighlight(cardFitur4, true);
        setCardHighlight(cardFitur5, true);
        setCardHighlight(cardFitur6, true);
    }

    @FXML
    public void handleFilterHarian() {
        setActiveFilterTab(btnFilterHarian);
        setCardHighlight(cardFitur1, true);
        setCardHighlight(cardFitur2, false);
        setCardHighlight(cardFitur3, true);
        setCardHighlight(cardFitur4, false);
        setCardHighlight(cardFitur5, false);
        setCardHighlight(cardFitur6, false);
    }

    @FXML
    public void handleFilterNutrisi() {
        setActiveFilterTab(btnFilterNutrisi);
        setCardHighlight(cardFitur1, false);
        setCardHighlight(cardFitur2, true);
        setCardHighlight(cardFitur3, false);
        setCardHighlight(cardFitur4, true);
        setCardHighlight(cardFitur5, false);
        setCardHighlight(cardFitur6, false);
    }

    @FXML
    public void handleFilterLaporan() {
        setActiveFilterTab(btnFilterLaporan);
        setCardHighlight(cardFitur1, false);
        setCardHighlight(cardFitur2, false);
        setCardHighlight(cardFitur3, false);
        setCardHighlight(cardFitur4, false);
        setCardHighlight(cardFitur5, true);
        setCardHighlight(cardFitur6, true);
    }

    private void setActiveFilterTab(Button activeBtn) {
        Button[] filters = {btnFilterAll, btnFilterHarian, btnFilterNutrisi, btnFilterLaporan};
        for (Button b : filters) {
            if (b != null) b.getStyleClass().remove("btn-filter-pill-active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("btn-filter-pill-active")) {
            activeBtn.getStyleClass().add("btn-filter-pill-active");
        }
    }

    private void setCardHighlight(VBox card, boolean active) {
        if (card != null) {
            double target = active ? 1.0 : 0.25;
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(260), card);
            ft.setToValue(target);
            ft.setInterpolator(AnimationHelper.GSAP_POWER2_OUT);
            ft.play();
        }
    }

    // Interactive Card Clicks
    @FXML public void handleCardFitur1Click() {
        DialogHelper.showInfo("Tantangan Sehat Harian",
                "Fitur ini memberikan tantangan harian adaptif sesuai rutinitas Anda.\n\n" +
                "Contoh: Minum 2 gelas air setelah bangun, 15 menit jalan santai, hingga peregangan leher saat WFH.\n\n" +
                "Klik 'Create Account' untuk mulai menyelesaikan tantangan dan kumpulkan poin konsistensi!");
    }

    @FXML public void handleCardFitur2Click() {
        DialogHelper.showInfo("Pelacakan Kebiasaan",
                "Pantau 3 pilar hidup sehat harian:\n" +
                "1. Hidrasi (target 2.000 ml)\n" +
                "2. Isi Piringku (Sayur, Buah, Karbohidrat & Protein)\n" +
                "3. Menit Gerak Aktif\n\n" +
                "Catat dengan satu sentuhan mudah setiap hari!");
    }

    @FXML public void handleCardFitur3Click() {
        DialogHelper.showInfo("Rekomendasi Aktivitas",
                "Algoritma SehatHarian mengkalibrasi waktu luang Anda untuk merekomendasikan jenis latihan yang aman dan efektif tanpa membebani jadwal harian Anda.");
    }

    @FXML public void handleCardFitur4Click() {
        DialogHelper.showInfo("Jadwal & Pengingat Tidur",
                "Fitur pengingat waktu tidur terintegrasi alarm audio tenang untuk membantu menjaga ritme sirkadian dan kualitas pemulihan tubuh Anda di malam hari.");
    }

    @FXML public void handleCardFitur5Click() {
        DialogHelper.showInfo("Grafik Konsistensi",
                "Visualisasi kemajuan 7 hari berturut-turut untuk menjaga api motivasi hidup sehat tanpa rasa bersalah jika terlewat.");
    }

    @FXML public void handleCardFitur6Click() {
        DialogHelper.showInfo("Evaluasi Mingguan",
                "Setiap akhir pekan, SehatHarian merangkum kepatuhan rutinitas Anda menjadi Skor Sehat (0-100) lengkap dengan saran perbaikan untuk minggu berikutnya.");
    }

    // =========================================================================
    // INTERAKTIF WIDGET 4: FAQ ACCORDION
    // =========================================================================
    @FXML public void handleToggleFaq1() { toggleFaqItem(faqContent1, lblFaqArrow1); }
    @FXML public void handleToggleFaq2() { toggleFaqItem(faqContent2, lblFaqArrow2); }
    @FXML public void handleToggleFaq3() { toggleFaqItem(faqContent3, lblFaqArrow3); }
    @FXML public void handleToggleFaq4() { toggleFaqItem(faqContent4, lblFaqArrow4); }

    private void toggleFaqItem(VBox content, Label arrow) {
        if (content == null || arrow == null) return;
        boolean currentlyOpen = content.isVisible();
        boolean willOpen = !currentlyOpen;
        content.setVisible(willOpen);
        content.setManaged(willOpen);
        arrow.setText(willOpen ? "▼" : "▶");
        if (willOpen) {
            AnimationHelper.fadeIn(content, 260);
        }
    }

    // =========================================================================
    // NAVBAR & SCROLLING CONTROLS
    // =========================================================================
    private void updateActiveNav(Button activeButton) {
        Button[] navButtons = {btnNavBeranda, btnNavCoba, btnNavFitur, btnNavTentang, btnNavBantuan};
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("hero-nav-btn-active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("hero-nav-btn-active")) {
            activeButton.getStyleClass().add("hero-nav-btn-active");
        }
    }

    @FXML
    public void handleNavBeranda() {
        updateActiveNav(btnNavBeranda);
        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(0.0);
        }
    }

    @FXML
    public void handleNavCoba() {
        updateActiveNav(btnNavCoba);
        scrollToNode(interactiveDemoSectionBox);
    }

    @FXML
    public void handleNavFitur() {
        updateActiveNav(btnNavFitur);
        scrollToNode(fiturSectionBox);
    }

    @FXML
    public void handleNavTentang() {
        updateActiveNav(btnNavTentang);
        scrollToNode(deskripsiSectionBox);
    }

    @FXML
    public void handleNavBantuan() {
        updateActiveNav(btnNavBantuan);
        scrollToNode(faqSectionBox);
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
    // ACTIONS & DIALOGS
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
    public void handleAppDistributionInfo() {
        DialogHelper.showInfo("Informasi Distribusi Aplikasi SehatHarian",
                "Status Ketersediaan Saat Ini:\n\n" +
                "1. Platform Desktop & Web (JPro): Aktif dan dapat langsung digunakan secara penuh melalui browser atau aplikasi desktop.\n\n" +
                "2. Aplikasi Android & iOS: Sedang dalam tahap rilis pengujian internal (Closed Beta v2.4.3).\n\n" +
                "Anda dapat langsung membuat akun melalui tombol 'Buat Akun Sekarang' untuk mulai mencatat kebiasaan harian Anda tanpa menunggu rilis toko aplikasi!");
    }

    @FXML
    public void handleContactEmail() {
        DialogHelper.showInfo("Pusat Bantuan & Dukungan",
                "Tim Layanan Pengguna SehatHarian siap membantu Anda!\n\n" +
                "Email Resmi: bantuan@sehatharian.id\n" +
                "Jam Operasional: Senin - Jumat, 08.00 - 17.00 WIB\n\n" +
                "Kirimkan pertanyaan terkait fitur, panduan gizi harian, atau kendala teknis akun Anda.");
    }

    @FXML
    public void handleFooterPrivacy() {
        DialogHelper.showInfo("Kebijakan Privasi",
                "Keamanan Data Anda adalah Prioritas Kami:\n\n" +
                "• Catatan kesehatan harian, hidrasi, dan jadwal tidur Anda tersimpan secara aman dalam database pribadi Anda.\n" +
                "• SehatHarian tidak membagikan atau menjual data kebiasaan pribadi kepada pihak ketiga.");
    }

    @FXML
    public void handleFooterTerms() {
        DialogHelper.showInfo("Syarat & Ketentuan",
                "Pedoman Penggunaan Platform:\n\n" +
                "1. Seluruh fitur tantangan dan target harian dirancang untuk membangun rutinitas sehat secara mandiri.\n" +
                "2. Konten dan rekomendasi tidak dimaksudkan sebagai pengganti konsultasi, resep, atau perawatan medis resmi dokter.");
    }
}