package com.sehatharian.controller;

import com.sehatharian.model.EvaluasiMingguan;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.EvaluasiService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class EvaluasiMingguanController {

    @FXML private VBox evaluasiRoot;
    @FXML private Label labelPeriodeEvaluasi;
    @FXML private Label labelSkorEvaluasi;
    @FXML private Label labelGradeEvaluasi;
    @FXML private Label labelStatusTidur;
    @FXML private Label labelRataTidur;
    @FXML private Label labelRingkasanTeks;
    @FXML private Label labelRekomendasiTeks;

    @FXML private TableView<EvaluasiMingguan> tableArsipEvaluasi;
    @FXML private TableColumn<EvaluasiMingguan, String> colMingguMulai;
    @FXML private TableColumn<EvaluasiMingguan, String> colSkor;
    @FXML private TableColumn<EvaluasiMingguan, String> colRataTidur;
    @FXML private TableColumn<EvaluasiMingguan, String> colStandarTidur;
    @FXML private TableColumn<EvaluasiMingguan, String> colTanggalDibuat;

    private final EvaluasiService service = new EvaluasiService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;

    private final ObservableList<EvaluasiMingguan> listArsip = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        muatDataPengguna();
        muatEvaluasi();
        AnimationHelper.fadeIn(evaluasiRoot, 350);
    }

    private void setupTableColumns() {
        colMingguMulai.setCellValueFactory(data -> 
            new SimpleStringProperty("Minggu " + DateHelper.formatTanggalPendek(data.getValue().getMingguMulai())));
        colSkor.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%.1f%%", data.getValue().getSkorKonsistensi())));
        colRataTidur.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%.1f Jam", data.getValue().getDurasiTidurRataRata())));
        colStandarTidur.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isMemenuhiStandarTidur() ? "Memenuhi" : "Kurang"));
        colStandarTidur.setCellFactory(param -> new TableCell<EvaluasiMingguan, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean ok = "Memenuhi".equals(item);
                    Label badge = new Label(ok ? "Memenuhi (≥ 5h)" : "Kurang (< 5h)");
                    badge.getStyleClass().setAll(ok ? "badge-selesai" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colTanggalDibuat.setCellValueFactory(data -> {
            if (data.getValue().getDibuatPada() != null) {
                return new SimpleStringProperty(DateHelper.formatTanggalPendek(data.getValue().getDibuatPada().toLocalDate()));
            }
            return new SimpleStringProperty("-");
        });

        // Setup placeholder data kosong
        VBox emptyBox = new VBox(6);
        emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 24 10;");
        Label emptyTitle = new Label("Belum Ada Riwayat Evaluasi");
        emptyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px; -fx-text-fill: #34483F;");
        Label emptySub = new Label("Jalankan evaluasi mingguan untuk melihat analisis menyeluruh konsistensi hidup sehat.");
        emptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6D63;");
        emptyBox.getChildren().addAll(emptyTitle, emptySub);
        tableArsipEvaluasi.setPlaceholder(emptyBox);

        tableArsipEvaluasi.setItems(listArsip);

        tableArsipEvaluasi.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tampilkanDetailEvaluasi(newVal);
            }
        });
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
        } catch (SQLException ignored) {}
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }
    }

    public void muatEvaluasi() {
        if (penggunaAktif == null) return;
        try {
            // Ambil daftar arsip
            List<EvaluasiMingguan> daftar = service.getDaftarEvaluasi(penggunaAktif.getId());
            listArsip.setAll(daftar);

            if (!daftar.isEmpty()) {
                tampilkanDetailEvaluasi(daftar.get(0));
            } else {
                // Tampilkan info belum ada data
                labelPeriodeEvaluasi.setText("Belum ada evaluasi tersimpan di database.");
                labelSkorEvaluasi.setText("-");
                labelGradeEvaluasi.setText("Belum Dievaluasi");
                labelStatusTidur.setText("Belum Ada Data");
                labelStatusTidur.getStyleClass().setAll("badge-dilewati");
                labelRataTidur.setText("-");
                labelRingkasanTeks.setText("Sistem belum memiliki evaluasi yang tersimpan. Klik tombol 'Jalankan Evaluasi Sekarang' untuk menganalisis 7 hari terakhir.");
                labelRekomendasiTeks.setText("• Rekomendasi otomatis akan ditampilkan setelah evaluasi dijalankan.");
            }
        } catch (Exception e) {
            System.err.println("Info muat evaluasi: " + e.getMessage());
        }
    }

    private void tampilkanDetailEvaluasi(EvaluasiMingguan em) {
        labelPeriodeEvaluasi.setText("Periode: 7 hari s.d. " + DateHelper.formatTanggalPendek(em.getMingguMulai().plusDays(6)));
        labelSkorEvaluasi.setText(String.format("%.0f%%", em.getSkorKonsistensi()));
        labelGradeEvaluasi.setText(em.getGradeKonsistensi());

        if (em.isMemenuhiStandarTidur()) {
            labelStatusTidur.setText("Memenuhi Standar Minimal (≥ 5 Jam)");
            labelStatusTidur.getStyleClass().setAll("badge-selesai");
        } else {
            labelStatusTidur.setText("Di Bawah Standar PRD (< 5 Jam)");
            labelStatusTidur.getStyleClass().setAll("badge-warning");
        }

        labelRataTidur.setText(String.format("Rata-rata: %.1f jam / malam", em.getDurasiTidurRataRata()));
        labelRingkasanTeks.setText(em.getRingkasan());
        labelRekomendasiTeks.setText(em.getRekomendasi());
    }

    @FXML
    public void handleJalankanEvaluasi() {
        if (penggunaAktif == null) return;
        try {
            EvaluasiMingguan em = service.buatEvaluasiMingguan(penggunaAktif.getId(), LocalDate.now());
            muatEvaluasi();
            DialogHelper.showSuccess("Evaluasi Berhasil Dijalankan", 
                "Analisis 7 hari terakhir berhasil dijalankan dan disimpan ke MySQL!\n" +
                "Skor konsistensi Anda: " + String.format("%.1f%%", em.getSkorKonsistensi()));
        } catch (SQLException e) {
            DialogHelper.showError("Error", "Gagal menjalankan evaluasi: " + e.getMessage());
        }
    }
}
