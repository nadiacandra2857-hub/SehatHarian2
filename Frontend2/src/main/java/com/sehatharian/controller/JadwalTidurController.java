package com.sehatharian.controller;

import com.sehatharian.model.JadwalTidur;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
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

public class JadwalTidurController implements AutoCloseable {
    private final UserSession session;
    private final com.sehatharian.model.SessionAccess access;
    private final com.sehatharian.util.WriteAction writes;
    private final long viewGeneration;
    private boolean released;
    private Runnable detachReminder = () -> {};

    public JadwalTidurController(UserSession session, PengingatTidurService reminders) {
        this.session = java.util.Objects.requireNonNull(session);
        this.access = new com.sehatharian.model.SessionAccess(session);
        this.writes = new com.sehatharian.util.WriteAction(access);
        this.evaluasiService = new EvaluasiService(access);
        this.tidurService = new TidurService(access);
        this.pengingatService = java.util.Objects.requireNonNull(reminders);
        this.viewGeneration = session.getGeneration();
    }


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
    @FXML private Label nightTidurLabel;
    @FXML private Label nightBangunLabel;
    @FXML private Label nightDurasiLabel;
    @FXML private Label nightStatusLabel;
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

    private final TidurService tidurService;
    private final EvaluasiService evaluasiService;
    private final PengingatTidurService pengingatService;

    private Pengguna penggunaAktif;
    private final ObservableList<JadwalTidur> listRiwayatTidur = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        access.owner();
        datePickerTanggal.setValue(LocalDate.now());

        try {
            var imgUrl = getClass().getResource("/images/illustration_sleep.png");
            if (imgUrl != null) {
                // EditorialImage retains the UI-1 atlas crop.
            }
        } catch (Exception ignored) {}

        inisialisasiTimePickers();
        setupTableColumns();
        setupEmptyPlaceholder();
        muatDataPengguna();
        detachReminder = pengingatService.subscribe(message -> {
            if (released || viewGeneration != session.getGeneration()) return;
            labelJadwalAktifInfo.setText(pengingatService.getStatus());
            updateStatusPengingatUI();
        });
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
                    Label badge = new Label(ok ? "Rencana ≥ 5 jam" : "Rencana < 5 jam");
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
            { btnHapus.getStyleClass().add("write-command"); }
            {
                btnHapus.getStyleClass().add("btn-danger");
                btnHapus.setOnAction(event -> {
                    access.owner();
                    JadwalTidur j = getTableView().getItems().get(getIndex());
                    if (DialogHelper.showConfirmation(tidurRoot, "Konfirmasi Hapus", "Hapus catatan tidur tanggal " + DateHelper.formatTanggalPendek(j.getTanggal()) + "?")) {
                        try {
                            writes.submit(tidurRoot, () -> tidurService.hapusCatatan(j.getId()), () -> {
                            muatRiwayatTidur();
            });
                        } catch (SQLException e) {
            access.owner();
                            DialogHelper.showError(tidurRoot, "Kendala Database", "Layanan penyimpanan belum tersedia. Data belum dikonfirmasi; silakan coba lagi.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else { btnHapus.setDisable(writes.reviewRequired()); setGraphic(btnHapus); }
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
        penggunaAktif = session.requireActiveUser();
    }

    public void muatRiwayatTidur() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || penggunaAktif == null || !session.isLoggedIn() || session.getUserId() != penggunaAktif.getId()) return;
        pengingatService.reloadSchedule();
        labelJadwalAktifInfo.setText(pengingatService.getStatus());
        updateStatusPengingatUI();
        try {
            List<JadwalTidur> list = tidurService.getRiwayatTidur(penggunaAktif.getId(), 30);
            listRiwayatTidur.setAll(list);
            labelJumlahRiwayat.setText(list.size() + " Catatan Tersimpan");

            tableRiwayatTidur.setPlaceholder(new Label("Belum ada jadwal tersimpan."));
            nightTidurLabel.setText("—"); nightBangunLabel.setText("—"); nightDurasiLabel.setText("—");
            nightStatusLabel.setText("Belum ada jadwal tersimpan; tidur aktual belum dicatat.");
            JadwalTidur terbaru = tidurService.getJadwalTerbaru(penggunaAktif.getId());
            if (terbaru != null) {
                labelJadwalAktifInfo.setText(pengingatService.getStatus());
                nightTidurLabel.setText(DateHelper.formatJamMenit(terbaru.getJamTidur()));
                nightBangunLabel.setText(DateHelper.formatJamMenit(terbaru.getJamBangun()));
                nightDurasiLabel.setText(String.format("%.1f jam", terbaru.getDurasiJam()));
                nightStatusLabel.setText(terbaru.isMemenuhiStandar()
                    ? "Rencana ≥ 5 jam · bukan tidur aktual · " + terbaru.getTanggal()
                    : "Rencana < 5 jam · bukan tidur aktual · " + terbaru.getTanggal());
                // Reminder target comes exclusively from the strict SQL projection, never this legacy history.
                updateStatusPengingatUI();
            }
        } catch (Exception e) {
            access.owner();
            listRiwayatTidur.clear(); labelJumlahRiwayat.setText("Gagal dimuat");
            nightTidurLabel.setText("—"); nightBangunLabel.setText("—"); nightDurasiLabel.setText("—");
            nightStatusLabel.setText("Jadwal gagal dimuat. Gunakan Perbarui Riwayat untuk mencoba kembali.");
            tableRiwayatTidur.setPlaceholder(new Label("Riwayat jadwal gagal dimuat."));
        }
    }

    @FXML
    public void handleHitungDurasiPreview() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || !session.isLoggedIn()) return;
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
                badgePreviewStandar.setText("Rencana ≥ 5 jam");
                badgePreviewStandar.getStyleClass().setAll("badge-selesai");
                labelWarningKurangTidur.setVisible(false);
                labelWarningKurangTidur.setManaged(false);
            } else {
                badgePreviewStandar.setText("Rencana < 5 jam");
                badgePreviewStandar.getStyleClass().setAll("badge-warning");
                labelWarningKurangTidur.setVisible(true);
                labelWarningKurangTidur.setManaged(true);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleSimpanTidur() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || !session.isLoggedIn()) return;
        try {
            LocalDate tgl = datePickerTanggal.getValue();
            if (tgl == null) {
                DialogHelper.showWarning(tidurRoot, "Input Kurang Lengkap", "Pilih tanggal catatan istirahat.");
                return;
            }

            int hTidur = Integer.parseInt(comboJamTidur.getValue());
            int mTidur = Integer.parseInt(comboMenitTidur.getValue());
            int hBangun = Integer.parseInt(comboJamBangun.getValue());
            int mBangun = Integer.parseInt(comboMenitBangun.getValue());

            LocalTime tTidur = LocalTime.of(hTidur, mTidur);
            LocalTime tBangun = LocalTime.of(hBangun, mBangun);
            boolean aktifkanPengingat = chkAktifkanPengingat.isSelected();

            writes.submitResult(tidurRoot,
                () -> tidurService.catatDenganEvaluasi(penggunaAktif.getId(), tgl, tTidur, tBangun, aktifkanPengingat, evaluasiService),
                result -> {
                    if (result.confirmed()) {
                        DialogHelper.showSuccess(tidurRoot, "Tersimpan", result.steps().isEmpty()
                            ? "Catatan tidur berhasil disimpan." : "Catatan tidur dan evaluasi berhasil disimpan.");
                        muatRiwayatTidur();
                    } else if (result.status() == com.sehatharian.storage.WriteResult.Status.PARSIAL) {
                        DialogHelper.showWarning(tidurRoot, "Tersimpan Sebagian", "Catatan tidur sudah tersimpan. "
                            + (result.needsReview() ? "Hasil evaluasi belum pasti; periksa riwayat evaluasi." : "Evaluasi belum selesai; buka halaman Evaluasi untuk tindak lanjut.")
                            + " Jangan simpan ulang catatan tidur ini.");
                        muatRiwayatTidur();
                    } else DialogHelper.showWarning(tidurRoot, "Hasil Penyimpanan", result.message());
                });
        } catch (Exception e) {
            access.owner();
            DialogHelper.showError(tidurRoot, "Gagal Menyimpan", e.getMessage());
        }
    }

    @FXML
    public void handleTogglePengingat() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || !session.isLoggedIn()) return;
        boolean sekarang = pengingatService.isAktif();
        pengingatService.setAktif(!sekarang);
        updateStatusPengingatUI();
    }

    private void updateStatusPengingatUI() {
        if (pengingatService.getTargetJamTidur() == null) {
            btnTogglePengingat.setText("Jadwal pengingat belum tersedia (WIB)");
            btnTogglePengingat.getStyleClass().setAll("btn-outline");
            return;
        }
        boolean aktif = pengingatService.isAktif();
        if (aktif) {
            btnTogglePengingat.setText("Pengingat aktif · Nonaktifkan");
            btnTogglePengingat.getStyleClass().setAll("btn-success");
        } else {
            btnTogglePengingat.setText("Pengingat nonaktif · Aktifkan");
            btnTogglePengingat.getStyleClass().setAll("btn-outline");
        }
    }

    @FXML
    public void handleUjiAlarm() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || !session.isLoggedIn()) return;
        pengingatService.ujiCobaPengingat();
    }

    @FXML
    public void handleMuatRiwayat() {
        access.owner();
        if (released || viewGeneration != session.getGeneration() || !session.isLoggedIn()) return;
        muatRiwayatTidur();
    }
    @Override public void close() {
        access.close();
        if (released) return;
        released = true;
        detachReminder.run();
    }
}
