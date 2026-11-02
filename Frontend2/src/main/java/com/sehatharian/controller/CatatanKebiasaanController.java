package com.sehatharian.controller;

import com.sehatharian.model.Aktivitas;
import com.sehatharian.model.Hidrasi;
import com.sehatharian.model.KebiasaanMakan;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.KebiasaanService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.IconFactory;
import com.sehatharian.util.SoundEffects;
import com.sehatharian.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CatatanKebiasaanController implements AutoCloseable {
    private final UserSession session;
    private final com.sehatharian.model.SessionAccess access;
    private final com.sehatharian.util.WriteAction writes;

    private Dialog<Void> exerciseDialog;
    private final SoundEffects.View sounds;

    public CatatanKebiasaanController(UserSession session, SoundEffects effects) {
        this.session = java.util.Objects.requireNonNull(session);
        this.access = new com.sehatharian.model.SessionAccess(session);
        this.writes = new com.sehatharian.util.WriteAction(access);
        this.service = new KebiasaanService(access);
        this.sounds = java.util.Objects.requireNonNull(effects).forView();
    }


    @FXML private VBox kebiasaanRoot;
    @FXML private DatePicker datePickerTanggal;

    // Hidrasi
    @FXML private ImageView waterImageView;
    @FXML private Label labelTotalHidrasi;
    @FXML private ProgressBar progressBarHidrasi;
    @FXML private javafx.scene.layout.HBox waterGlassesBox;
    @FXML private javafx.scene.control.TabPane tabPaneKebiasaan;
    @FXML private TextField txtCustomHidrasi;
    @FXML private TextField txtCatatanHidrasi;
    @FXML private TableView<Hidrasi> tableHidrasi;
    @FXML private TableColumn<Hidrasi, String> colHidrasiJumlah;
    @FXML private TableColumn<Hidrasi, String> colHidrasiCatatan;
    @FXML private TableColumn<Hidrasi, Void> colHidrasiAksi;

    // Aktivitas
    @FXML private ImageView activityImageView;
    @FXML private Label labelTotalAktivitas;
    @FXML private Label labelAktivitasStatus;
    @FXML private ComboBox<String> comboPresetAktivitas;
    @FXML private TextField txtDurasiAktivitas;
    @FXML private TextField txtKeteranganAktivitas;
    @FXML private TableView<Aktivitas> tableAktivitas;
    @FXML private TableColumn<Aktivitas, String> colAktivitasJenis;
    @FXML private TableColumn<Aktivitas, Integer> colAktivitasDurasi;
    @FXML private TableColumn<Aktivitas, String> colAktivitasKeterangan;
    @FXML private TableColumn<Aktivitas, Void> colAktivitasAksi;

    // Makan
    @FXML private ImageView mealImageView;
    @FXML private ComboBox<String> comboWaktuMakan;
    @FXML private TextField txtNamaMenu;
    @FXML private TextField txtKeteranganMakan;
    @FXML private TableView<KebiasaanMakan> tableMakan;
    @FXML private TableColumn<KebiasaanMakan, String> colMakanWaktu;
    @FXML private TableColumn<KebiasaanMakan, String> colMakanMenu;
    @FXML private TableColumn<KebiasaanMakan, String> colMakanKeterangan;
    @FXML private TableColumn<KebiasaanMakan, Void> colMakanAksi;

    private final KebiasaanService service;
    private Pengguna penggunaAktif;
    private LocalDate tanggalDipilih;

    private final ObservableList<Hidrasi> listHidrasi = FXCollections.observableArrayList();
    private final ObservableList<Aktivitas> listAktivitas = FXCollections.observableArrayList();
    private final ObservableList<KebiasaanMakan> listMakan = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        access.owner();
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);

        inisialisasiGambar();
        inisialisasiFormInputs();
        setupTableColumns();

        muatDataPengguna();
        muatSemuaData();
        pasangIkonTab();
        AnimationHelper.fadeIn(kebiasaanRoot, 350);
    }

    private void pasangIkonTab() {
        if (tabPaneKebiasaan == null) return;
        String[] ikon = {"drop", "run", "utensils"};
        String[] warna = {"svg-icon-blue", "svg-icon", "svg-icon-orange"};
        for (int i = 0; i < ikon.length && i < tabPaneKebiasaan.getTabs().size(); i++) {
            tabPaneKebiasaan.getTabs().get(i).setGraphic(IconFactory.icon(ikon[i], 16, warna[i]));
        }
    }

    private void inisialisasiGambar() {
        try {
            var waterUrl = getClass().getResource("/images/illustration_water.png");


            var actUrl = getClass().getResource("/images/illustration_stretch.png");


            var mealUrl = getClass().getResource("/images/illustration_food.png");

        } catch (Exception ignored) {}
    }

    private void inisialisasiFormInputs() {
        comboPresetAktivitas.setEditable(true);
        comboPresetAktivitas.setItems(FXCollections.observableArrayList(
            "Jalan Santai",
            "Jalan Cepat",
            "Peregangan Otot Meja Kerja",
            "Workout Penguat Inti Ringan",
            "Senam Kebugaran Lansia",
            "Bersepeda Statis / Santai",
            "Latihan Keseimbangan Berdiri",
            "Lari Ringan / Jogging"
        ));

        comboWaktuMakan.setItems(FXCollections.observableArrayList(
            "Sarapan",
            "Makan Siang",
            "Makan Malam",
            "Camilan Sehat"
        ));
        comboWaktuMakan.setValue("Sarapan");
    }

    private void setupTableColumns() {
        // Kolom Hidrasi
        colHidrasiJumlah.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getJumlahMl() + " ml (" + String.format("%.2f", data.getValue().getJumlahLiter()) + " L)"));
        colHidrasiCatatan.setCellValueFactory(new PropertyValueFactory<>("catatan"));
        setupAksiHapusColumn(colHidrasiAksi, item -> {
            try {
                writes.submit(kebiasaanRoot, () -> service.hapusHidrasi(item.getId()), () -> {
                muatHidrasi();
            });
            } catch (SQLException e) {
            access.owner();
                DialogHelper.showError(kebiasaanRoot, "Error", "Layanan penyimpanan belum tersedia. Data belum dikonfirmasi; silakan coba lagi.");
            }
        });
        tableHidrasi.setItems(listHidrasi);

        // Kolom Aktivitas
        colAktivitasJenis.setCellValueFactory(new PropertyValueFactory<>("jenisAktivitas"));
        colAktivitasDurasi.setCellValueFactory(new PropertyValueFactory<>("durasiMenit"));
        colAktivitasKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        setupAksiHapusColumn(colAktivitasAksi, item -> {
            try {
                writes.submit(kebiasaanRoot, () -> service.hapusAktivitas(item.getId()), () -> {
                muatAktivitas();
            });
            } catch (SQLException e) {
            access.owner();
                DialogHelper.showError(kebiasaanRoot, "Error", "Layanan penyimpanan belum tersedia. Data belum dikonfirmasi; silakan coba lagi.");
            }
        });
        tableAktivitas.setItems(listAktivitas);

        // Kolom Makan
        colMakanWaktu.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLabelWaktuMakan()));
        colMakanMenu.setCellValueFactory(new PropertyValueFactory<>("menu"));
        colMakanKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        setupAksiHapusColumn(colMakanAksi, item -> {
            try {
                writes.submit(kebiasaanRoot, () -> service.hapusMakan(item.getId()), () -> {
                muatMakan();
            });
            } catch (SQLException e) {
            access.owner();
                DialogHelper.showError(kebiasaanRoot, "Error", "Layanan penyimpanan belum tersedia. Data belum dikonfirmasi; silakan coba lagi.");
            }
        });
        tableMakan.setItems(listMakan);
    }

    private <T> void setupAksiHapusColumn(TableColumn<T, Void> column, java.util.function.Consumer<T> deleteAction) {
        column.setCellFactory(param -> new TableCell<>() {
            private final Button btnHapus = new Button("Hapus");
            { btnHapus.getStyleClass().add("write-command"); }
            {
                btnHapus.getStyleClass().add("btn-danger");
                btnHapus.setOnAction(event -> {
                    access.owner();
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        T item = getTableView().getItems().get(index);
                        if (DialogHelper.showConfirmation(kebiasaanRoot, "Konfirmasi Hapus", "Yakin ingin menghapus catatan ini?")) {
                            deleteAction.accept(item);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    btnHapus.setDisable(writes.reviewRequired());
                    setGraphic(btnHapus);
                }
            }
        });
    }

    private void muatDataPengguna() {
        penggunaAktif = session.requireActiveUser();
    }

    public void muatSemuaData() {
        access.owner();
        muatHidrasi();
        muatAktivitas();
        muatMakan();
    }

    /** Delapan gelas (@250 ml): terisi sesuai total hidrasi riil hari yang dipilih. */
    private void gambarGelas(int totalMl) {
        if (waterGlassesBox == null) return;
        int terisi = Math.min(8, totalMl / 250);
        waterGlassesBox.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            waterGlassesBox.getChildren().add(IconFactory.icon("drop", 20, i < terisi ? "svg-icon-blue" : "glass-empty"));
        }
    }

    private void muatHidrasi() {
        if (penggunaAktif == null) return;
        try {
            List<Hidrasi> data = service.getHidrasiHariIni(penggunaAktif.getId(), tanggalDipilih);
            listHidrasi.setAll(data);
            tableHidrasi.setPlaceholder(new Label("Belum ada catatan minum pada tanggal ini."));
            int total = service.getTotalHidrasiMl(penggunaAktif.getId(), tanggalDipilih);
            labelTotalHidrasi.setText(data.isEmpty() ? "—" : String.format("%,d", total));
            double rasio = Math.min(total / 2000.0, 1.0);
            AnimationHelper.animateProgressBar(progressBarHidrasi, rasio);
            gambarGelas(total);
        } catch (SQLException e) {
            access.owner();
            listHidrasi.clear(); labelTotalHidrasi.setText("—"); progressBarHidrasi.setProgress(0); gambarGelas(0);
            tableHidrasi.setPlaceholder(new Label("Gagal memuat catatan minum. Pilih tanggal kembali untuk mencoba."));
        }
    }

    private void muatAktivitas() {
        if (penggunaAktif == null) return;
        try {
            List<Aktivitas> data = service.getAktivitasHariIni(penggunaAktif.getId(), tanggalDipilih);
            listAktivitas.setAll(data);
            tableAktivitas.setPlaceholder(new Label("Belum ada aktivitas pada tanggal ini."));
            int total = service.getTotalDurasiAktivitas(penggunaAktif.getId(), tanggalDipilih);
            labelTotalAktivitas.setText(data.isEmpty() ? "—" : String.valueOf(total));
            if (data.isEmpty()) {
                labelAktivitasStatus.setText("Belum ada catatan; belum dapat menilai aktivitas hari ini.");
            } else if (total >= 30) {
                labelAktivitasStatus.setText("Luar biasa! Target aktivitas fisik harian minimal 30 menit sudah tercapai.");
            } else {
                labelAktivitasStatus.setText("Saran: Tambah " + (30 - total) + " menit lagi untuk mencapai rekomendasi harian 30 menit.");
            }
        } catch (SQLException e) {
            access.owner();
            listAktivitas.clear(); labelTotalAktivitas.setText("—");
            labelAktivitasStatus.setText("Aktivitas gagal dimuat. Pilih tanggal kembali untuk mencoba.");
            tableAktivitas.setPlaceholder(new Label("Gagal memuat aktivitas."));
        }
    }

    private void muatMakan() {
        if (penggunaAktif == null) return;
        try {
            List<KebiasaanMakan> data = service.getMakanHariIni(penggunaAktif.getId(), tanggalDipilih);
            listMakan.setAll(data);
            tableMakan.setPlaceholder(new Label("Belum ada menu pada tanggal ini."));
        } catch (SQLException e) {
            access.owner();
            listMakan.clear(); tableMakan.setPlaceholder(new Label("Gagal memuat menu. Pilih tanggal kembali untuk mencoba."));
        }
    }

    // --- HANDLER HIDRASI ---
    @FXML public void handleQuick250() {
        access.owner(); tambahHidrasiDirect(250, "Air putih 1 gelas"); }
    @FXML public void handleQuick500() {
        access.owner(); tambahHidrasiDirect(500, "Air putih botol sedang 500 ml"); }
    @FXML public void handleQuick600() {
        access.owner(); tambahHidrasiDirect(600, "Air putih botol 600 ml"); }

    private void tambahHidrasiDirect(int ml, String catatan) {
        try {
            writes.submit(kebiasaanRoot, () -> { service.tambahHidrasi(penggunaAktif.getId(), tanggalDipilih, ml, catatan); return null; }, () -> {
            sounds.play(SoundEffects.Efek.AIR);
            muatHidrasi();
            });
        } catch (Exception e) {
            access.owner();
            DialogHelper.showError(kebiasaanRoot, "Error", e.getMessage());
        }
    }

    @FXML
    public void handleTambahHidrasiManual() {
        access.owner();
        try {
            String txt = txtCustomHidrasi.getText();
            if (txt == null || txt.trim().isEmpty()) {
                DialogHelper.showWarning(kebiasaanRoot, "Input Kurang", "Masukkan jumlah mililiter air terlebih dahulu.");
                return;
            }
            int ml = Integer.parseInt(txt.trim());
            String catatan = txtCatatanHidrasi.getText();
            writes.submit(kebiasaanRoot, () -> { service.tambahHidrasi(penggunaAktif.getId(), tanggalDipilih, ml, catatan); return null; }, () -> {
            sounds.play(SoundEffects.Efek.AIR);
            txtCustomHidrasi.clear();
            txtCatatanHidrasi.clear();
            muatHidrasi();
            });
        } catch (NumberFormatException e) {
            DialogHelper.showError(kebiasaanRoot, "Input Tidak Valid", "Jumlah air harus berupa angka bulat (misal 300).");
        } catch (Exception e) {
            access.owner();
            DialogHelper.showError(kebiasaanRoot, "Error", e.getMessage());
        }
    }

    /** Reference selection only; never writes, selects a duration, or marks completion. */
    public void pilihLatihan(com.sehatharian.catalog.Exercise exercise) {
        access.owner();
        comboPresetAktivitas.setValue(java.util.Objects.requireNonNull(exercise).name());
        if (labelAktivitasStatus != null) labelAktivitasStatus.setText("Nama dari RepDB dipilih. Masukkan durasi aktual lalu simpan jika sudah dilakukan.");
    }

    @FXML public void handleBukaKatalogLatihan() {
        access.owner();
        if (exerciseDialog != null) return;
        Dialog<Void> dialog = new Dialog<>();
        exerciseDialog = dialog;
        dialog.setTitle("Katalog latihan · RepDB");
        if (kebiasaanRoot.getScene() != null) dialog.initOwner(kebiasaanRoot.getScene().getWindow());
        var content = new com.sehatharian.util.ExerciseCatalogPane(com.sehatharian.catalog.ExerciseCatalog.local(), exercise -> {
            pilihLatihan(exercise); dialog.close();
        });
        ScrollPane scroll = new ScrollPane(content);scroll.setFitToWidth(true);scroll.setMinWidth(0);scroll.setId("exerciseScroll");
        scroll.getStyleClass().add("ui2");
        var pane=dialog.getDialogPane();pane.setContent(scroll);pane.getButtonTypes().add(ButtonType.CLOSE);
        ((Button)pane.lookupButton(ButtonType.CLOSE)).setText("Tutup katalog");
        pane.getStyleClass().add("ui2-dialog");pane.getStylesheets().add(getClass().getResource("/css/ui2.css").toExternalForm());
        double width=dialog.getOwner()==null?1000:dialog.getOwner().getWidth();
        double height=dialog.getOwner()==null?800:dialog.getOwner().getHeight();
        pane.setMinWidth(0);pane.setPrefWidth(Math.max(280,Math.min(1050,width-24)));
        scroll.setPrefViewportHeight(Math.max(200,Math.min(700,height-150)));
        Runnable detach=session.onIdentityChanged(dialog::close);
        dialog.setOnHidden(event->{content.close();detach.run();exerciseDialog=null;});
        dialog.show();
    }

    // --- HANDLER AKTIVITAS ---
    @FXML
    public void handleSimpanAktivitas() {
        access.owner();
        try {
            String jenis = comboPresetAktivitas.isEditable() ? comboPresetAktivitas.getEditor().getText() : comboPresetAktivitas.getValue();
            if (jenis == null || jenis.trim().isEmpty()) {
                DialogHelper.showWarning(kebiasaanRoot, "Pilihan Kosong", "Pilih jenis aktivitas terlebih dahulu.");
                return;
            }
            String durasiStr = txtDurasiAktivitas.getText();
            if (durasiStr == null || durasiStr.trim().isEmpty()) {
                DialogHelper.showWarning(kebiasaanRoot, "Input Kurang", "Masukkan durasi aktivitas dalam menit.");
                return;
            }
            int durasi = Integer.parseInt(durasiStr.trim());
            String ket = txtKeteranganAktivitas.getText();

            writes.submit(kebiasaanRoot, () -> { service.tambahAktivitas(penggunaAktif.getId(), tanggalDipilih, jenis, durasi, ket); return null; }, () -> {
            sounds.play(SoundEffects.Efek.SUKSES);
            txtDurasiAktivitas.clear();
            txtKeteranganAktivitas.clear();
            muatAktivitas();
            DialogHelper.showSuccess(kebiasaanRoot, "Tersimpan", "Catatan aktivitas fisik berhasil disimpan!");
            });
        } catch (NumberFormatException e) {
            DialogHelper.showError(kebiasaanRoot, "Input Tidak Valid", "Durasi aktivitas harus berupa angka menit (misal 20).");
        } catch (Exception e) {
            access.owner();
            DialogHelper.showError(kebiasaanRoot, "Error", e.getMessage());
        }
    }

    // --- HANDLER MAKAN ---
    @FXML
    public void handleSimpanMakan() {
        access.owner();
        try {
            String waktu = comboWaktuMakan.getValue();
            String dbWaktu = "sarapan";
            if ("Makan Siang".equalsIgnoreCase(waktu)) dbWaktu = "makan_siang";
            else if ("Makan Malam".equalsIgnoreCase(waktu)) dbWaktu = "makan_malam";
            else if ("Camilan Sehat".equalsIgnoreCase(waktu)) dbWaktu = "camilan";

            String menu = txtNamaMenu.getText();
            String ket = txtKeteranganMakan.getText();

            final String waktuPilihan = dbWaktu;
            writes.submit(kebiasaanRoot, () -> { service.tambahMakan(penggunaAktif.getId(), tanggalDipilih, waktuPilihan, menu, ket); return null; }, () -> {
            sounds.play(SoundEffects.Efek.SUKSES);
            txtNamaMenu.clear();
            txtKeteranganMakan.clear();
            muatMakan();
            DialogHelper.showSuccess(kebiasaanRoot, "Tersimpan", "Catatan pola makan berhasil disimpan!");
            });
        } catch (Exception e) {
            access.owner();
            DialogHelper.showError(kebiasaanRoot, "Error", e.getMessage());
        }
    }

    @FXML
    public void handleUbahTanggal() {
        access.owner();
        if (datePickerTanggal.getValue() != null) {
            tanggalDipilih = datePickerTanggal.getValue();
            muatSemuaData();
        }
    }

    @FXML
    public void handlePilihHariIni() {
        access.owner();
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);
        muatSemuaData();
    }
    @Override public void close() {
        if (exerciseDialog != null) exerciseDialog.close();
        access.close(); sounds.close(); }
}
