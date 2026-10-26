package com.sehatharian.controller;

import com.sehatharian.model.JadwalTidur;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RiwayatTantangan;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.*;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DashboardController {

    @FXML private VBox dashboardRoot;
    @FXML private Label heroDateLabel;
    @FXML private Label heroGreetingLabel;
    @FXML private Label heroQuoteLabel;
    @FXML private Label heroStatusTantanganLabel;
    @FXML private ImageView heroImageView;
    @FXML private VBox challengesContainer;

    @FXML private HBox preferenceBannerBox;
    @FXML private Label prefPersonalizedNoteLabel;
    @FXML private Label prefWaktuLuangBadge;
    @FXML private Label prefAktivitasBadge;
    @FXML private Label prefKelompokUmurBadge;

    @FXML private ImageView recHighlightImageView;
    @FXML private Label recHighlightTitleLabel;
    @FXML private Label recHighlightDescLabel;
    @FXML private Label recHighlightBadge;

    @FXML private Label hydrationCurrentLabel;
    @FXML private ProgressBar hydrationProgressBar;
    @FXML private Label hydrationStatusLabel;

    @FXML private Label activityCurrentLabel;
    @FXML private Label activitySummaryLabel;

    @FXML private Label sleepDurationLabel;
    @FXML private Label sleepStandardBadge;
    @FXML private Label weeklyScoreBadge;

    private MainLayoutController mainController;
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private final TantanganService tantanganService = new TantanganService();
    private final KebiasaanService kebiasaanService = new KebiasaanService();
    private final TidurService tidurService = new TidurService();
    private final EvaluasiService evaluasiService = new EvaluasiService();

    private Pengguna penggunaAktif;

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        muatDataPenggunaDanDashboard();
        muatAsetGambar();
        AnimationHelper.fadeIn(dashboardRoot, 400);
    }

    private void muatAsetGambar() {
        try {
            var heroUrl = getClass().getResource("/images/hero_wellness.png");
            if (heroUrl != null) {
                heroImageView.setImage(new Image(heroUrl.toExternalForm()));
            }
            var stretchUrl = getClass().getResource("/images/illustration_stretch.png");
            if (stretchUrl != null) {
                recHighlightImageView.setImage(new Image(stretchUrl.toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat gambar dashboard: " + e.getMessage());
        }
    }

    public void muatDataPenggunaDanDashboard() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.getActiveUser() != null) {
            penggunaAktif = session.getActiveUser();
        } else if (session.isLoggedIn() && session.getUserId() > 0) {
            try {
                penggunaAktif = penggunaRepo.getById(session.getUserId());
            } catch (Exception ignored) {}
        }

        if (penggunaAktif == null) {
            try {
                penggunaAktif = penggunaRepo.getDefaultPengguna();
            } catch (Exception e) {
                System.err.println("Gagal memuat pengguna default: " + e.getMessage());
            }
        }
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Pengguna Sehat", LocalDate.of(2000, 1, 1), "dewasa", "luar_rumah", 30);
        }

        // Sinkronisasi nama tampilan dari UserSession aktif bila ada
        if (session.isLoggedIn() && session.getUserName() != null && !session.getUserName().trim().isEmpty()) {
            penggunaAktif.setNama(session.getUserName());
        }

        // Tampilkan Banner Preferensi Onboarding
        if (preferenceBannerBox != null) {
            prefWaktuLuangBadge.setText(session.getWaktuLuangLabel());
            prefAktivitasBadge.setText("INDOOR".equalsIgnoreCase(session.getJenisAktivitas()) ? "Indoor (Dalam Ruangan)" : "Outdoor (Luar Ruangan)");
            prefKelompokUmurBadge.setText(session.getKelompokUmur());
        }

        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // 1. Hero Data
            heroDateLabel.setText(DateHelper.formatTanggalLengkap(today));
            heroGreetingLabel.setText(DateHelper.getSalamWaktu(now) + ", " + penggunaAktif.getNama() + "!");
            heroQuoteLabel.setText(DateHelper.getMotivasiHarian(now));

            // Rekomendasi Sorotan Adaptif berdasarkan Preferensi Onboarding
            if ("INDOOR".equalsIgnoreCase(session.getJenisAktivitas())) {
                recHighlightTitleLabel.setText("Peregangan Relaksasi Otot Rumah (Indoor)");
                recHighlightDescLabel.setText("Luangkan 10-15 menit peregangan mobilitas di dalam rumah untuk meredakan ketegangan leher dan punggung.");
                recHighlightBadge.setText("15 Menit • Fleksibilitas Indoor");
            } else {
                recHighlightTitleLabel.setText("Jalan Cepat Udara Terbuka (Outdoor)");
                recHighlightDescLabel.setText("Nikmati udara segar dengan berjalan kaki 20-30 menit di taman atau lingkungan sekitar rumah.");
                recHighlightBadge.setText("20 Menit • Kardio Outdoor");
            }

            // 2. Tantangan Hari Ini
            List<RiwayatTantangan> daftarTantangan = tantanganService.pastikanTantanganHariIni(penggunaAktif, today);
            tampilkanTantanganHariIni(daftarTantangan);

            // 3. Hidrasi
            int totalMl = kebiasaanService.getTotalHidrasiMl(penggunaAktif.getId(), today);
            hydrationCurrentLabel.setText(String.format("%,d", totalMl));
            double targetMl = 2000.0;
            double progressHidrasi = Math.min(totalMl / targetMl, 1.0);
            AnimationHelper.animateProgressBar(hydrationProgressBar, progressHidrasi);
            int persenHidrasi = (int) (progressHidrasi * 100);
            hydrationStatusLabel.setText("Asupan cairan tubuh sudah " + persenHidrasi + "% tercapai.");

            // 4. Aktivitas
            int totalMenit = kebiasaanService.getTotalDurasiAktivitas(penggunaAktif.getId(), today);
            activityCurrentLabel.setText(String.valueOf(totalMenit));
            if (totalMenit > 0) {
                activitySummaryLabel.setText("Hebat! Anda telah beraktivitas fisik " + totalMenit + " menit hari ini.");
            } else {
                activitySummaryLabel.setText("Belum ada aktivitas fisik tercatat hari ini.");
            }

            // 5. Tidur Terakhir
            JadwalTidur tidurTerakhir = tidurService.getJadwalTerbaru(penggunaAktif.getId());
            if (tidurTerakhir != null) {
                sleepDurationLabel.setText(String.format("%.1f", tidurTerakhir.getDurasiJam()));
                if (tidurTerakhir.isMemenuhiStandar()) {
                    sleepStandardBadge.setText("Memenuhi Standar (≥ 5 jam)");
                    sleepStandardBadge.getStyleClass().setAll("badge-selesai");
                } else {
                    sleepStandardBadge.setText("Kurang Standar (< 5 jam)");
                    sleepStandardBadge.getStyleClass().setAll("badge-warning");
                }
            } else {
                sleepDurationLabel.setText("-");
                sleepStandardBadge.setText("Belum ada jadwal/catatan tidur");
                sleepStandardBadge.getStyleClass().setAll("badge-dilewati");
            }

            // 6. Konsistensi Mingguan Skor
            var listKonsistensi = evaluasiService.getKonsistensi7HariTerakhir(penggunaAktif.getId(), today);
            double totalSkor = 0;
            for (var rk : listKonsistensi) {
                totalSkor += rk.getSkorHarianPersen();
            }
            double rataRata = Math.round((totalSkor / 7.0) * 10.0) / 10.0;
            weeklyScoreBadge.setText(String.format("%.0f%%", rataRata));

        } catch (Exception e) {
            System.err.println("Info muat data dashboard: " + e.getMessage());
        }
    }

    private void tampilkanTantanganHariIni(List<RiwayatTantangan> daftar) {
        challengesContainer.getChildren().clear();

        if (daftar.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 16; -fx-background-color: #FAFBF9; -fx-background-radius: 12;");
            Label lbl = new Label("Belum ada tantangan aktif hari ini. Klik 'Kerjakan Tantangan' untuk memulai!");
            lbl.getStyleClass().add("card-subtitle");
            emptyBox.getChildren().add(lbl);
            challengesContainer.getChildren().add(emptyBox);
            heroStatusTantanganLabel.setText("0 tantangan disarankan");
            return;
        }

        int selesaiCount = 0;
        for (RiwayatTantangan rt : daftar) {
            if (rt.isSelesai()) selesaiCount++;

            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-padding: 12 16; -fx-background-color: #FAFBF9; -fx-background-radius: 12; -fx-border-color: #E6EDE9; -fx-border-radius: 12;");

            VBox info = new VBox(3);
            HBox.setHgrow(info, Priority.ALWAYS);

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label lblJudul = new Label(rt.getTantanganJudul());
            lblJudul.setStyle("-fx-font-weight: 600; -fx-font-size: 13.5px; -fx-text-fill: #1E3129;");
            Label lblBadge = new Label(rt.getTantanganKategori() + " • " + rt.getDurasiTargetMenit() + " Menit");
            lblBadge.getStyleClass().add("badge-category");
            titleRow.getChildren().addAll(lblJudul, lblBadge);

            Label lblDesc = new Label(rt.getTantanganDeskripsi());
            lblDesc.getStyleClass().add("card-subtitle");
            lblDesc.setWrapText(true);

            info.getChildren().addAll(titleRow, lblDesc);

            Button btnAction = new Button();
            if (rt.isSelesai()) {
                btnAction.setText("Selesai");
                btnAction.getStyleClass().setAll("btn-success");
                btnAction.setDisable(true);
            } else if (rt.isDilewati()) {
                btnAction.setText("Dilewati");
                btnAction.getStyleClass().setAll("btn-outline");
                btnAction.setDisable(true);
            } else {
                btnAction.setText("Tandai Selesai");
                btnAction.getStyleClass().setAll("btn-primary");
                btnAction.setOnAction(ev -> {
                    try {
                        tantanganService.selesaikanTantangan(rt.getId());
                        AnimationHelper.scaleButtonFeedback(btnAction);
                        AnimationHelper.pulseSuccess(card);
                        muatDataPenggunaDanDashboard();
                    } catch (SQLException ex) {
                        DialogHelper.showError("Error", "Gagal memperbarui status tantangan: " + ex.getMessage());
                    }
                });
            }

            card.getChildren().addAll(info, btnAction);
            challengesContainer.getChildren().add(card);
        }

        heroStatusTantanganLabel.setText(selesaiCount + " dari " + daftar.size() + " tantangan selesai hari ini");
    }

    @FXML
    public void handleQuickAddHydration() {
        try {
            kebiasaanService.tambahHidrasi(penggunaAktif.getId(), LocalDate.now(), 250, "Quick add 1 gelas (+250 ml)");
            muatDataPenggunaDanDashboard();
        } catch (Exception e) {
            DialogHelper.showError("Gagal", "Gagal menambah catatan hidrasi: " + e.getMessage());
        }
    }

    @FXML public void handleBukaTantangan() { if (mainController != null) mainController.handleNavTantangan(); }
    @FXML public void handleBukaRekomendasi() { if (mainController != null) mainController.handleNavRekomendasi(); }
    @FXML public void handleBukaKebiasaan() { if (mainController != null) mainController.handleNavKebiasaan(); }
    @FXML public void handleBukaTidur() { if (mainController != null) mainController.handleNavTidur(); }
    @FXML public void handleBukaStatistik() { if (mainController != null) mainController.handleNavStatistik(); }
}
