package com.sehatharian.controller;

import com.sehatharian.model.JadwalTidur;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.EvaluasiService;
import com.sehatharian.service.PengingatTidurService;
import com.sehatharian.service.TidurService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class JadwalTidurController {

    @FXML private VBox tidurRoot;
    @FXML private ImageView sleepHeaderImageView;

    @FXML private DatePicker datePickerTanggal;
    @FXML private ComboBox<String> comboJamTidur;
    @FXML private ComboBox<String> comboMenitTidur;
    @FXML private ComboBox<String> comboJamBangun;
    @FXML private ComboBox<String> comboMenitBangun;

    @FXML private Label labelLintasTengahMalam;
    @FXML private Label labelPreviewDurasi;
    @FXML private Label badgePreviewStandar;
    @FXML private Label labelWarningKurangTidur;
    @FXML private CheckBox chkAktifkanPengingat;

    @FXML private Label labelJadwalAktifInfo;
    @FXML private Button btnTogglePengingat;

    @FXML private Label labelJumlahRiwayat;
    @FXML private TableView<JadwalTidur> tableRiwayatTidur;
    @FXML private TableColumn<JadwalTidur, String> colTanggal;
    @FXML private TableColumn<JadwalTidur, String> colJamTidur;
    @FXML private TableColumn<JadwalTidur, String> colJamBangun;
    @FXML private TableColumn<JadwalTidur, String> colDurasi;
    @FXML private TableColumn<JadwalTidur, Void> colStandar;
    @FXML private TableColumn<JadwalTidur, Void> colPengingat;
    @FXML private TableColumn<JadwalTidur, Void> colAksi;

    private final TidurService tidurService = new TidurService();
    private final EvaluasiService evaluasiService = new EvaluasiService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private final PengingatTidurService pengingatService = PengingatTidurService.getInstance();

    private Pengguna penggunaAktif;
    private final ObservableList<JadwalTidur> listRiwayatTidur = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        datePickerTanggal.setValue(LocalDate.now());

        try {
            var imgUrl = getClass().getResource("/images/illustration_sleep.png");
            if (imgUrl != null) {
                sleepHeaderImageView.setImage(new Image(imgUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        inisialisasiTimePickers();
        setupTableColumns();
        setupEmptyPlaceholder();
        muatDataPengguna();
        muatRiwayatTidur();
        updateStatusPengingatUI();
        handleHitungDurasiPreview();

        AnimationHelper.fadeIn(tidurRoot, 350);
    }

    private void inisialisasiTimePickers() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i += 5) {
            minutes.add(String.format("%02d", i));
        }

        comboJamTidur.setItems(hours);
        comboMenitTidur.setItems(minutes);
        comboJamBangun.setItems(hours);
        comboMenitBangun.setItems(minutes);

        // Nilai bawaan ramah kebiasaan: 22:30 tidur, 06:00 bangun (7.5 jam)
        comboJamTidur.setValue("22");
        comboMenitTidur.setValue("30");
        comboJamBangun.setValue("06");
        comboMenitBangun.setValue("00");
    }

    private void setupTableColumns() {
        colTanggal.setCellValueFactory(data -> new SimpleStringProperty(DateHelper.formatTanggalPendek(data.getValue().getTanggal())));
        colJamTidur.setCellValueFactory(data -> new SimpleStringProperty(DateHelper.formatJamMenit(data.getValue().getJamTidur())));
        colJamBangun.setCellValueFactory(data -> new SimpleStringProperty(DateHelper.formatJamMenit(data.getValue().getJamBangun())));
        colDurasi.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f Jam", data.getValue().getDurasiJam())));

        // Badge Standar tanpa emoji
        colStandar.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    JadwalTidur jt = getTableView().getItems().get(getIndex());
                    boolean ok = jt.isMemenuhiStandar();
                    Label badge = new Label(ok ? "Memenuhi Standar (≥ 5h)" : "Kurang Istirahat (< 5h)");
                    badge.getStyleClass().setAll(ok ? "badge-selesai" : "badge-warning");
                    setGraphic(badge);
                }
            }
        });

        // Badge Status Pengingat tanpa emoji
        colPengingat.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    JadwalTidur jt = getTableView().getItems().get(getIndex());
                    boolean aktif = jt.isPengingatAktif();
                    Label badge = new Label(aktif ? "Aktif Harian" : "Nonaktif");
                    badge.getStyleClass().setAll(aktif ? "badge-category" : "badge-dilewati");
                    setGraphic(badge);
                }
            }
        });

        // Tombol Aksi Hapus dengan konfirmasi
        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnHapus = new Button("Hapus");
            {
                btnHapus.getStyleClass().add("btn-danger");
                btnHapus.setOnAction(event -> {
                    JadwalTidur j = getTableView().getItems().get(getIndex());
                    if (DialogHelper.showConfirmation("Konfirmasi Hapus", "Hapus catatan tidur tanggal " + DateHelper.formatTanggalPendek(j.getTanggal()) + "?")) {
                        try {
                            tidurService.hapusCatatan(j.getId());
                            muatRiwayatTidur();
                        } catch (SQLException e) {
                            DialogHelper.showError("Kendala Database", "Gagal menghapus catatan: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnHapus);
            }
        });

        tableRiwayatTidur.setItems(listRiwayatTidur);
    }

    private void setupEmptyPlaceholder() {
        VBox emptyBox = new VBox(6);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 30 10;");
        Label title = new Label("Belum Ada Riwayat Istirahat Tersimpan");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #34483F;");
        Label sub = new Label("Catat waktu tidur dan bangun pada formulir di atas untuk memulai pemantauan pola istirahat.");
        sub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #5A6D63;");
        emptyBox.getChildren().addAll(title, sub);
        tableRiwayatTidur.setPlaceholder(emptyBox);
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

    public void muatRiwayatTidur() {
        if (penggunaAktif == null) return;
        try {
            List<JadwalTidur> list = tidurService.getRiwayatTidur(penggunaAktif.getId(), 30);
            listRiwayatTidur.setAll(list);
            labelJumlahRiwayat.setText(list.size() + " Catatan Tersimpan");

            JadwalTidur terbaru = tidurService.getJadwalTerbaru(penggunaAktif.getId());
            if (terbaru != null) {
                labelJadwalAktifInfo.setText("Pukul " + DateHelper.formatJamMenit(terbaru.getJamTidur()) + " WIB");
                pengingatService.updateJadwal(terbaru.getJamTidur(), terbaru.isPengingatAktif());
                updateStatusPengingatUI();
            }
        } catch (Exception e) {
            System.err.println("Info muat riwayat tidur: " + e.getMessage());
        }
    }

    @FXML
    public void handleHitungDurasiPreview() {
        try {
            int hTidur = Integer.parseInt(comboJamTidur.getValue());
            int mTidur = Integer.parseInt(comboMenitTidur.getValue());
            int hBangun = Integer.parseInt(comboJamBangun.getValue());
            int mBangun = Integer.parseInt(comboMenitBangun.getValue());

            LocalTime tTidur = LocalTime.of(hTidur, mTidur);
            LocalTime tBangun = LocalTime.of(hBangun, mBangun);

            // Indikator lintas hari
            if (tBangun.isBefore(tTidur) || tBangun.equals(tTidur)) {
                labelLintasTengahMalam.setText("Jadwal melintasi tengah malam: mulai istirahat malam ini, bangun esok hari (+1 hari).");
            } else {
                labelLintasTengahMalam.setText("Jadwal dalam hari yang sama: istirahat dan bangun pada tanggal kalender yang sama.");
            }

            double durasi = tidurService.hitungDurasiTidur(tTidur, tBangun);
            labelPreviewDurasi.setText(String.format("%.1f Jam", durasi));

            boolean standar = tidurService.apakahMemenuhiStandar(durasi);
            if (standar) {
                badgePreviewStandar.setText("Memenuhi Standar Minimal (≥ 5 Jam)");
                badgePreviewStandar.getStyleClass().setAll("badge-selesai");
                labelWarningKurangTidur.setVisible(false);
                labelWarningKurangTidur.setManaged(false);
            } else {
                badgePreviewStandar.setText("Di Bawah Standar Minimal (< 5 Jam)");
                badgePreviewStandar.getStyleClass().setAll("badge-warning");
                labelWarningKurangTidur.setVisible(true);
                labelWarningKurangTidur.setManaged(true);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleSimpanTidur() {
        try {
            LocalDate tgl = datePickerTanggal.getValue();
            if (tgl == null) {
                DialogHelper.showWarning("Input Kurang Lengkap", "Pilih tanggal catatan istirahat.");
                return;
            }

            int hTidur = Integer.parseInt(comboJamTidur.getValue());
            int mTidur = Integer.parseInt(comboMenitTidur.getValue());
            int hBangun = Integer.parseInt(comboJamBangun.getValue());
            int mBangun = Integer.parseInt(comboMenitBangun.getValue());

            LocalTime tTidur = LocalTime.of(hTidur, mTidur);
            LocalTime tBangun = LocalTime.of(hBangun, mBangun);
            boolean aktifkanPengingat = chkAktifkanPengingat.isSelected();

            JadwalTidur jt = tidurService.catatJamTidur(penggunaAktif.getId(), tgl, tTidur, tBangun, aktifkanPengingat);

            // Update pengingat di background scheduler jika diaktifkan
            if (aktifkanPengingat) {
                pengingatService.updateJadwal(tTidur, true);
            }

            // Jika durasi < 5 jam, jalankan evaluasi mingguan otomatis sesuai PRD (F-07 & F-05)
            if (!jt.isMemenuhiStandar()) {
                evaluasiService.buatEvaluasiMingguan(penggunaAktif.getId(), tgl);
                DialogHelper.showWarning("Evaluasi Khusus Dijalankan", 
                    "Durasi istirahat Anda tercatat " + String.format("%.1f", jt.getDurasiJam()) + " jam (< 5 jam).\n" +
                    "Sistem telah menjalankan analisis evaluasi mingguan dan menyiapkan rekomendasi pemulihan istirahat.");
            } else {
                DialogHelper.showSuccess("Berhasil Disimpan", "Catatan tidur berhasil disimpan ke database MySQL.");
            }

            muatRiwayatTidur();
        } catch (Exception e) {
            DialogHelper.showError("Gagal Menyimpan", e.getMessage());
        }
    }

    @FXML
    public void handleTogglePengingat() {
        boolean sekarang = pengingatService.isAktif();
        pengingatService.setAktif(!sekarang);
        updateStatusPengingatUI();
    }

    private void updateStatusPengingatUI() {
        boolean aktif = pengingatService.isAktif();
        if (aktif) {
            btnTogglePengingat.setText("Status Alarm: Aktif (Klik untuk Nonaktifkan)");
            btnTogglePengingat.getStyleClass().setAll("btn-success");
        } else {
            btnTogglePengingat.setText("Status Alarm: Nonaktif (Klik untuk Aktifkan)");
            btnTogglePengingat.getStyleClass().setAll("btn-outline");
        }
    }

    @FXML
    public void handleUjiAlarm() {
        pengingatService.ujiCobaPengingat();
    }

    @FXML
    public void handleMuatRiwayat() {
        muatRiwayatTidur();
    }
}
