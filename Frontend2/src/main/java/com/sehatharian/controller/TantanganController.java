package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RiwayatTantangan;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.TantanganService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TantanganController {

    @FXML private VBox tantanganRoot;
    @FXML private DatePicker datePickerTanggal;
    @FXML private Label labelProgressTitle;
    @FXML private Label labelProgressSubtitle;
    @FXML private Label labelProgressPercent;
    @FXML private ProgressBar progressBarTantangan;

    @FXML private Button btnFilterSemua;
    @FXML private Button btnFilterJalan;
    @FXML private Button btnFilterWorkout;
    @FXML private Button btnFilterPeregangan;
    @FXML private Button btnFilterHidrasi;

    @FXML private VBox cardsContainer;
    @FXML private VBox emptyStateBox;
    @FXML private ImageView emptyStateImageView;
    @FXML private Label labelEmptyTitle;
    @FXML private Label labelEmptySubtitle;
    @FXML private Button btnEmptyAction;

    private final TantanganService tantanganService = new TantanganService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;
    private LocalDate tanggalDipilih;
    private String filterKategori = "Semua";

    private List<RiwayatTantangan> daftarTantanganSemua = new ArrayList<>();

    @FXML
    public void initialize() {
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);

        try {
            var imgUrl = getClass().getResource("/images/illustration_walk.png");
            if (imgUrl != null) {
                emptyStateImageView.setImage(new Image(imgUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        muatDataPengguna();
        muatTantangan();
        AnimationHelper.fadeIn(tantanganRoot, 350);
    }

    private void muatDataPengguna() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.getActiveUser() != null) {
            penggunaAktif = session.getActiveUser();
            return;
        } else if (session.isLoggedIn() && session.getUserId() > 0) {
            try {
                penggunaAktif = penggunaRepo.getById(session.getUserId());
                if (penggunaAktif != null) return;
            } catch (Exception ignored) {}
        }

        try {
            penggunaAktif = penggunaRepo.getDefaultPengguna();
        } catch (Exception e) {
            System.err.println("Gagal memuat pengguna default: " + e.getMessage());
        }
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia Ayu Candra", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }
    }

    public void muatTantangan() {
        if (penggunaAktif == null) return;
        try {
            if (tanggalDipilih.equals(LocalDate.now())) {
                daftarTantanganSemua = tantanganService.pastikanTantanganHariIni(penggunaAktif, tanggalDipilih);
            } else {
                daftarTantanganSemua = tantanganService.getTantanganByTanggal(penggunaAktif.getId(), tanggalDipilih);
            }
            renderTantangan();
        } catch (Exception e) {
            System.err.println("Info muat tantangan: " + e.getMessage());
        }
    }

    private void renderTantangan() {
        cardsContainer.getChildren().clear();

        // Filter list sesuai tombol kategori aktif dengan proteksi null-safety
        List<RiwayatTantangan> filtered = new ArrayList<>();
        for (RiwayatTantangan rt : daftarTantanganSemua) {
            String kat = rt.getTantanganKategori() != null ? rt.getTantanganKategori().toLowerCase() : "";
            if ("Semua".equalsIgnoreCase(filterKategori)) {
                filtered.add(rt);
            } else if ("Jalan Kaki".equalsIgnoreCase(filterKategori) && kat.contains("jalan")) {
                filtered.add(rt);
            } else if ("Workout di Rumah".equalsIgnoreCase(filterKategori) && kat.contains("workout")) {
                filtered.add(rt);
            } else if ("Peregangan".equalsIgnoreCase(filterKategori) && kat.contains("peregangan")) {
                filtered.add(rt);
            } else if ("Hidrasi & Nutrisi".equalsIgnoreCase(filterKategori) && (kat.contains("hidrasi") || kat.contains("nutrisi"))) {
                filtered.add(rt);
            }
        }

        // Hitung progress penyelesaian keseluruhan hari ini
        int total = daftarTantanganSemua.size();
        int selesai = 0;
        for (RiwayatTantangan rt : daftarTantanganSemua) {
            if (rt.isSelesai()) selesai++;
        }

        labelProgressTitle.setText("Kemajuan Tantangan " + DateHelper.formatTanggalPendek(tanggalDipilih));
        if (total == 0) {
            labelProgressSubtitle.setText("Belum ada target tantangan untuk tanggal ini");
        } else if (selesai == total) {
            labelProgressSubtitle.setText("Luar biasa! Seluruh " + total + " tantangan berhasil diselesaikan");
        } else {
            labelProgressSubtitle.setText(selesai + " dari " + total + " tantangan berhasil diselesaikan");
        }

        double rasio = total > 0 ? (double) selesai / total : 0.0;
        AnimationHelper.animateProgressBar(progressBarTantangan, rasio);
        labelProgressPercent.setText(String.format("%.0f%%", rasio * 100));

        // Penanganan Empty State
        if (filtered.isEmpty()) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);

            if (daftarTantanganSemua.isEmpty()) {
                if (labelEmptyTitle != null) {
                    labelEmptyTitle.setText("Belum Ada Tantangan untuk Tanggal Ini");
                }
                if (labelEmptySubtitle != null) {
                    labelEmptySubtitle.setText("Sistem belum menyiapkan tantangan pada tanggal ini. Klik tombol di bawah untuk membuat tantangan adaptif.");
                }
                if (btnEmptyAction != null) {
                    btnEmptyAction.setText("Muat Tantangan Rekomendasi");
                }
            } else {
                if (labelEmptyTitle != null) {
                    labelEmptyTitle.setText("Tidak Ada Tantangan dalam Kategori Ini");
                }
                if (labelEmptySubtitle != null) {
                    labelEmptySubtitle.setText("Tantangan tersedia di kategori lain. Klik tombol di bawah untuk menampilkan seluruh tantangan.");
                }
                if (btnEmptyAction != null) {
                    btnEmptyAction.setText("Tampilkan Semua Tantangan");
                }
            }
            return;
        } else {
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
        }

        // Render setiap kartu tantangan
        for (RiwayatTantangan rt : filtered) {
            VBox card = new VBox(12);
            card.getStyleClass().add("wellness-card");

            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox info = new VBox(5);
            HBox.setHgrow(info, Priority.ALWAYS);

            HBox badgeRow = new HBox(8);
            badgeRow.setAlignment(Pos.CENTER_LEFT);

            Label badgeKat = new Label(rt.getTantanganKategori() != null ? rt.getTantanganKategori().toUpperCase() : "UMUM");
            badgeKat.getStyleClass().add("badge-category");

            Label badgeDurasi = new Label(rt.getDurasiTargetMenit() + " Menit");
            badgeDurasi.getStyleClass().add("badge-category");

            Label badgeStatus = new Label();
            if (rt.isSelesai()) {
                badgeStatus.setText("SELESAI");
                badgeStatus.getStyleClass().add("badge-selesai");
            } else if (rt.isDilewati()) {
                badgeStatus.setText("DILEWATI");
                badgeStatus.getStyleClass().add("badge-dilewati");
            } else {
                badgeStatus.setText("DISARANKAN");
                badgeStatus.getStyleClass().add("badge-disarankan");
            }

            badgeRow.getChildren().addAll(badgeKat, badgeDurasi, badgeStatus);

            Label lblJudul = new Label(rt.getTantanganJudul());
            lblJudul.getStyleClass().add("card-title");

            Label lblDesc = new Label(rt.getTantanganDeskripsi());
            lblDesc.getStyleClass().add("card-subtitle");
            lblDesc.setWrapText(true);

            info.getChildren().addAll(badgeRow, lblJudul, lblDesc);

            // Area Tombol Aksi yang Logis dan Konsisten
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            if (rt.isSelesai()) {
                Label lblWaktu = new Label();
                if (rt.getDiselesaikanPada() != null) {
                    lblWaktu.setText("Selesai pukul " + DateHelper.formatJamMenit(rt.getDiselesaikanPada().toLocalTime()) + " WIB");
                } else {
                    lblWaktu.setText("Telah diselesaikan");
                }
                lblWaktu.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #2E7D5B; -fx-font-weight: 600; -fx-padding: 0 8 0 0;");

                Button btnReset = new Button("Batalkan Selesai");
                btnReset.getStyleClass().add("btn-secondary");
                btnReset.setOnAction(e -> resetStatus(rt));

                actions.getChildren().addAll(lblWaktu, btnReset);

            } else if (rt.isDilewati()) {
                Button btnSelesai = new Button("Selesaikan Sekarang");
                btnSelesai.getStyleClass().add("btn-primary");
                btnSelesai.setOnAction(e -> selesaikan(rt, btnSelesai, card));

                Button btnReset = new Button("Reset Status");
                btnReset.getStyleClass().add("btn-secondary");
                btnReset.setOnAction(e -> resetStatus(rt));

                actions.getChildren().addAll(btnSelesai, btnReset);

            } else {
                // Status disarankan
                Button btnSelesai = new Button("Selesaikan");
                btnSelesai.getStyleClass().add("btn-primary");
                btnSelesai.setOnAction(e -> selesaikan(rt, btnSelesai, card));

                Button btnLewati = new Button("Lewati");
                btnLewati.getStyleClass().add("btn-outline");
                btnLewati.setOnAction(e -> lewati(rt));

                actions.getChildren().addAll(btnSelesai, btnLewati);
            }

            header.getChildren().addAll(info, actions);
            card.getChildren().add(header);
            cardsContainer.getChildren().add(card);
        }
    }

    private void selesaikan(RiwayatTantangan rt, Button btn, VBox card) {
        try {
            tantanganService.selesaikanTantangan(rt.getId());
            AnimationHelper.scaleButtonFeedback(btn);
            AnimationHelper.pulseSuccess(card);
            muatTantangan();
        } catch (SQLException ex) {
            DialogHelper.showError("Error", "Gagal memperbarui status tantangan: " + ex.getMessage());
        }
    }

    private void lewati(RiwayatTantangan rt) {
        try {
            tantanganService.lewatiTantangan(rt.getId());
            muatTantangan();
        } catch (SQLException ex) {
            DialogHelper.showError("Error", "Gagal melewati tantangan: " + ex.getMessage());
        }
    }

    private void resetStatus(RiwayatTantangan rt) {
        try {
            tantanganService.resetTantangan(rt.getId());
            muatTantangan();
        } catch (SQLException ex) {
            DialogHelper.showError("Error", "Gagal mereset status tantangan: " + ex.getMessage());
        }
    }

    @FXML
    public void handleUbahTanggal() {
        if (datePickerTanggal.getValue() != null) {
            tanggalDipilih = datePickerTanggal.getValue();
            muatTantangan();
        }
    }

    @FXML
    public void handlePilihHariIni() {
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);
        muatTantangan();
    }

    @FXML
    public void handleMuatUlangTantangan() {
        try {
            if (daftarTantanganSemua.isEmpty()) {
                tantanganService.pastikanTantanganHariIni(penggunaAktif, tanggalDipilih);
            }
            filterKategori = "Semua";
            setActiveFilterButton(btnFilterSemua);
            muatTantangan();
        } catch (SQLException e) {
            DialogHelper.showError("Error", "Gagal memuat tantangan: " + e.getMessage());
        }
    }

    private void setActiveFilterButton(Button activeButton) {
        Button[] buttons = {
            btnFilterSemua, btnFilterJalan, btnFilterWorkout, btnFilterPeregangan, btnFilterHidrasi
        };
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().setAll("btn-outline");
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().setAll("btn-primary");
        }
    }

    @FXML public void handleFilterSemua() { filterKategori = "Semua"; setActiveFilterButton(btnFilterSemua); renderTantangan(); }
    @FXML public void handleFilterJalan() { filterKategori = "Jalan Kaki"; setActiveFilterButton(btnFilterJalan); renderTantangan(); }
    @FXML public void handleFilterWorkout() { filterKategori = "Workout di Rumah"; setActiveFilterButton(btnFilterWorkout); renderTantangan(); }
    @FXML public void handleFilterPeregangan() { filterKategori = "Peregangan"; setActiveFilterButton(btnFilterPeregangan); renderTantangan(); }
    @FXML public void handleFilterHidrasi() { filterKategori = "Hidrasi & Nutrisi"; setActiveFilterButton(btnFilterHidrasi); renderTantangan(); }
}
