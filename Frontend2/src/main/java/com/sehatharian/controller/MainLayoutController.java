package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.PengingatTidurService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainLayoutController {

    @FXML private ImageView brandLogoImageView;
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavTantangan;
    @FXML private Button btnNavRekomendasi;
    @FXML private Button btnNavKebiasaan;
    @FXML private Button btnNavTidur;
    @FXML private Button btnNavStatistik;
    @FXML private Button btnNavEvaluasi;
    @FXML private Button btnNavProfil;

    @FXML private Label sidebarUserNameLabel;
    @FXML private Label sidebarUserTagLabel;

    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubtitleLabel;
    @FXML private Label headerDateBadgeLabel;

    @FXML private HBox bannerNotificationBox;
    @FXML private Label bannerNotificationText;

    @FXML private StackPane contentArea;

    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();

    @FXML
    public void initialize() {
        // 1. Logo
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                brandLogoImageView.setImage(new Image(logoUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        // 2. Date badge
        headerDateBadgeLabel.setText(DateHelper.formatTanggalLengkap(LocalDate.now()));

        // 3. User profile data
        muatDataPenggunaSidebar();

        // 4. Daftarkan listener pengingat tidur desktop (F-08)
        PengingatTidurService.getInstance().setNotifikasiListener(this::tampilkanNotifikasiBanner);

        // 5. Muat halaman Dashboard pertama kali
        handleNavDashboard();
    }

    public void muatDataPenggunaSidebar() {
        Pengguna p = null;
        try {
            p = penggunaRepo.getDefaultPengguna();
        } catch (Exception e) {
            System.err.println("Gagal memuat profil pengguna sidebar: " + e.getMessage());
        }
        if (p == null) {
            p = new Pengguna(1, "Nadia Ayu Candra", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }

        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.getUserName() != null && !session.getUserName().trim().isEmpty()) {
            p.setNama(session.getUserName());
        }
        updateUserInfoSidebar(p);
    }

    @FXML
    public void handleLogout() {
        UserSession.getInstance().logout();
        SceneNavigator.showLandingPage();
    }

    public void updateUserInfoSidebar(Pengguna p) {
        if (p == null) return;
        sidebarUserNameLabel.setText(p.getNama());
        sidebarUserTagLabel.setText(p.getLabelGolonganUsia() + " • " + 
            ("dalam_rumah".equalsIgnoreCase(p.getGolonganKesibukan()) ? "Di Dalam Rumah" : "Di Luar Rumah"));
    }

    public void tampilkanNotifikasiBanner(String pesan) {
        if (pesan == null || pesan.trim().isEmpty()) return;
        Platform.runLater(() -> {
            bannerNotificationText.setText(pesan);
            bannerNotificationBox.setVisible(true);
            bannerNotificationBox.setManaged(true);
            AnimationHelper.fadeIn(bannerNotificationBox, 300);
        });
    }

    @FXML
    public void handleCloseBanner() {
        bannerNotificationBox.setVisible(false);
        bannerNotificationBox.setManaged(false);
    }

    @FXML
    public void handleUjiAlarm() {
        PengingatTidurService.getInstance().ujiCobaPengingat();
    }

    // --- NAVIGASI HALAMAN ---

    @FXML
    public void handleNavDashboard() {
        setActiveNav(btnNavDashboard);
        headerTitleLabel.setText("Dashboard Kesehatan");
        headerSubtitleLabel.setText("Selamat datang! Pantau dan bangun kebiasaan sehat hari ini.");
        muatTampilan("/fxml/DashboardView.fxml", "Dashboard");
    }

    @FXML
    public void handleNavTantangan() {
        setActiveNav(btnNavTantangan);
        headerTitleLabel.setText("Tantangan Kesehatan Harian");
        headerSubtitleLabel.setText("Tantangan realistis yang disesuaikan dengan rutinitas harian dan kondisi fisik Anda.");
        muatTampilan("/fxml/TantanganView.fxml", "Tantangan");
    }

    @FXML
    public void handleNavRekomendasi() {
        setActiveNav(btnNavRekomendasi);
        headerTitleLabel.setText("Rekomendasi Aktivitas, Menu & Workout");
        headerSubtitleLabel.setText("Rekomendasi yang disaring berdasarkan waktu luang, kesibukan, dan kelompok usia.");
        muatTampilan("/fxml/RekomendasiView.fxml", "Rekomendasi");
    }

    @FXML
    public void handleNavKebiasaan() {
        setActiveNav(btnNavKebiasaan);
        headerTitleLabel.setText("Pelacak Kebiasaan Harian");
        headerSubtitleLabel.setText("Catat konsumsi hidrasi air putih, aktivitas fisik teratur, dan pola makan sehat.");
        muatTampilan("/fxml/CatatanKebiasaanView.fxml", "Kebiasaan");
    }

    @FXML
    public void handleNavTidur() {
        setActiveNav(btnNavTidur);
        headerTitleLabel.setText("Jadwal Tidur & Pengingat Alarm");
        headerSubtitleLabel.setText("Pencatatan jam tidur manual, standar minimal 5 jam, serta alarm desktop.");
        muatTampilan("/fxml/JadwalTidurView.fxml", "Tidur");
    }

    @FXML
    public void handleNavStatistik() {
        setActiveNav(btnNavStatistik);
        headerTitleLabel.setText("Grafik Konsistensi Mingguan");
        headerSubtitleLabel.setText("Visualisasi data aktual konsistensi kebiasaan sehat selama 7 hari terakhir.");
        muatTampilan("/fxml/StatistikMingguanView.fxml", "Statistik");
    }

    @FXML
    public void handleNavEvaluasi() {
        setActiveNav(btnNavEvaluasi);
        headerTitleLabel.setText("Evaluasi & Solusi Mingguan");
        headerSubtitleLabel.setText("Evaluasi menyeluruh 7 hari terakhir disertai rekomendasi dan pemulihan pola hidup.");
        muatTampilan("/fxml/EvaluasiMingguanView.fxml", "Evaluasi");
    }

    @FXML
    public void handleNavProfil() {
        setActiveNav(btnNavProfil);
        headerTitleLabel.setText("Profil & Pengaturan Pengguna");
        headerSubtitleLabel.setText("Kelola data profil, preferensi waktu luang, serta diagnostik koneksi database MySQL.");
        muatTampilan("/fxml/ProfilPengaturanView.fxml", "Profil");
    }

    private void setActiveNav(Button activeButton) {
        Button[] buttons = {
            btnNavDashboard, btnNavTantangan, btnNavRekomendasi, btnNavKebiasaan,
            btnNavTidur, btnNavStatistik, btnNavEvaluasi, btnNavProfil
        };
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void muatTampilan(String fxmlPath, String viewKey) {
        try {
            Node node = viewCache.get(viewKey);
            Object ctrl = controllerCache.get(viewKey);

            if (node == null || ctrl == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                node = loader.load();
                ctrl = loader.getController();

                if (node != null && ctrl != null) {
                    viewCache.put(viewKey, node);
                    controllerCache.put(viewKey, ctrl);
                }

                // Inject parent reference saat pertama kali dimuat
                if (ctrl instanceof DashboardController dc) {
                    dc.setMainController(this);
                } else if (ctrl instanceof ProfilPengaturanController pc) {
                    pc.setMainController(this);
                }
            }

            // Segarkan data halaman secara dinamis saat navigasi agar selalu up-to-date
            if (ctrl instanceof DashboardController dc) {
                dc.muatDataPenggunaDanDashboard();
            } else if (ctrl instanceof TantanganController tc) {
                tc.muatTantangan();
            } else if (ctrl instanceof CatatanKebiasaanController kc) {
                kc.muatSemuaData();
            } else if (ctrl instanceof JadwalTidurController jc) {
                jc.muatRiwayatTidur();
            } else if (ctrl instanceof StatistikMingguanController sc) {
                sc.muatDanTampilkanGrafik();
            } else if (ctrl instanceof EvaluasiMingguanController ec) {
                ec.muatEvaluasi();
            } else if (ctrl instanceof RekomendasiController rc) {
                rc.muatRekomendasi();
            }

            contentArea.getChildren().setAll(node);
            AnimationHelper.fadeIn(node, 250);

        } catch (IOException e) {
            System.err.println("Gagal memuat tampilan " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
