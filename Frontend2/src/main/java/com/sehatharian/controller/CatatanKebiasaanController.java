package com.sehatharian.controller;

import com.sehatharian.model.Aktivitas;
import com.sehatharian.model.Hidrasi;
import com.sehatharian.model.KebiasaanMakan;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.KebiasaanService;
import com.sehatharian.util.AnimationHelper;
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

public class CatatanKebiasaanController {

    @FXML private VBox kebiasaanRoot;
    @FXML private DatePicker datePickerTanggal;

    // Hidrasi
    @FXML private ImageView waterImageView;
    @FXML private Label labelTotalHidrasi;
    @FXML private ProgressBar progressBarHidrasi;
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

    private final KebiasaanService service = new KebiasaanService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;
    private LocalDate tanggalDipilih;

    private final ObservableList<Hidrasi> listHidrasi = FXCollections.observableArrayList();
    private final ObservableList<Aktivitas> listAktivitas = FXCollections.observableArrayList();
    private final ObservableList<KebiasaanMakan> listMakan = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);

        inisialisasiGambar();
        inisialisasiFormInputs();
        setupTableColumns();

        muatDataPengguna();
        muatSemuaData();
        AnimationHelper.fadeIn(kebiasaanRoot, 350);
    }

    private void inisialisasiGambar() {
        try {
            var waterUrl = getClass().getResource("/images/illustration_water.png");
            if (waterUrl != null) waterImageView.setImage(new Image(waterUrl.toExternalForm()));

            var actUrl = getClass().getResource("/images/illustration_stretch.png");
            if (actUrl != null) activityImageView.setImage(new Image(actUrl.toExternalForm()));

            var mealUrl = getClass().getResource("/images/illustration_food.png");
            if (mealUrl != null) mealImageView.setImage(new Image(mealUrl.toExternalForm()));
        } catch (Exception ignored) {}
    }

    private void inisialisasiFormInputs() {
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
                service.hapusHidrasi(item.getId());
                muatHidrasi();
            } catch (SQLException e) {
                DialogHelper.showError("Error", "Gagal menghapus log hidrasi: " + e.getMessage());
            }
        });
        tableHidrasi.setItems(listHidrasi);

        // Kolom Aktivitas
        colAktivitasJenis.setCellValueFactory(new PropertyValueFactory<>("jenisAktivitas"));
        colAktivitasDurasi.setCellValueFactory(new PropertyValueFactory<>("durasiMenit"));
        colAktivitasKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        setupAksiHapusColumn(colAktivitasAksi, item -> {
            try {
                service.hapusAktivitas(item.getId());
                muatAktivitas();
            } catch (SQLException e) {
                DialogHelper.showError("Error", "Gagal menghapus log aktivitas: " + e.getMessage());
            }
        });
        tableAktivitas.setItems(listAktivitas);

        // Kolom Makan
        colMakanWaktu.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLabelWaktuMakan()));
        colMakanMenu.setCellValueFactory(new PropertyValueFactory<>("menu"));
        colMakanKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        setupAksiHapusColumn(colMakanAksi, item -> {
            try {
                service.hapusMakan(item.getId());
                muatMakan();
            } catch (SQLException e) {
                DialogHelper.showError("Error", "Gagal menghapus log makanan: " + e.getMessage());
            }
        });
        tableMakan.setItems(listMakan);
    }

    private <T> void setupAksiHapusColumn(TableColumn<T, Void> column, java.util.function.Consumer<T> deleteAction) {
        column.setCellFactory(param -> new TableCell<>() {
            private final Button btnHapus = new Button("Hapus");
            {
                btnHapus.getStyleClass().add("btn-danger");
                btnHapus.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        T item = getTableView().getItems().get(index);
                        if (DialogHelper.showConfirmation("Konfirmasi Hapus", "Yakin ingin menghapus catatan ini?")) {
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
                    setGraphic(btnHapus);
                }
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

    public void muatSemuaData() {
        muatHidrasi();
        muatAktivitas();
        muatMakan();
    }

    private void muatHidrasi() {
        if (penggunaAktif == null) return;
        try {
            List<Hidrasi> data = service.getHidrasiHariIni(penggunaAktif.getId(), tanggalDipilih);
            listHidrasi.setAll(data);
            int total = service.getTotalHidrasiMl(penggunaAktif.getId(), tanggalDipilih);
            labelTotalHidrasi.setText(String.format("%,d", total));
            double rasio = Math.min(total / 2000.0, 1.0);
            AnimationHelper.animateProgressBar(progressBarHidrasi, rasio);
        } catch (SQLException e) {
            System.err.println("Gagal memuat hidrasi: " + e.getMessage());
        }
    }

    private void muatAktivitas() {
        if (penggunaAktif == null) return;
        try {
            List<Aktivitas> data = service.getAktivitasHariIni(penggunaAktif.getId(), tanggalDipilih);
            listAktivitas.setAll(data);
            int total = service.getTotalDurasiAktivitas(penggunaAktif.getId(), tanggalDipilih);
            labelTotalAktivitas.setText(String.valueOf(total));
            if (total >= 30) {
                labelAktivitasStatus.setText("Luar biasa! Target aktivitas fisik harian minimal 30 menit sudah tercapai.");
            } else {
                labelAktivitasStatus.setText("Saran: Tambah " + (30 - total) + " menit lagi untuk mencapai rekomendasi harian 30 menit.");
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat aktivitas: " + e.getMessage());
        }
    }

    private void muatMakan() {
        if (penggunaAktif == null) return;
        try {
            List<KebiasaanMakan> data = service.getMakanHariIni(penggunaAktif.getId(), tanggalDipilih);
            listMakan.setAll(data);
        } catch (SQLException e) {
            System.err.println("Gagal memuat kebiasaan makan: " + e.getMessage());
        }
    }

    // --- HANDLER HIDRASI ---
    @FXML public void handleQuick250() { tambahHidrasiDirect(250, "Air putih 1 gelas"); }
    @FXML public void handleQuick500() { tambahHidrasiDirect(500, "Air putih botol sedang 500 ml"); }
    @FXML public void handleQuick600() { tambahHidrasiDirect(600, "Air putih botol 600 ml"); }

    private void tambahHidrasiDirect(int ml, String catatan) {
        try {
            service.tambahHidrasi(penggunaAktif.getId(), tanggalDipilih, ml, catatan);
            muatHidrasi();
        } catch (Exception e) {
            DialogHelper.showError("Error", e.getMessage());
        }
    }

    @FXML
    public void handleTambahHidrasiManual() {
        try {
            String txt = txtCustomHidrasi.getText();
            if (txt == null || txt.trim().isEmpty()) {
                DialogHelper.showWarning("Input Kurang", "Masukkan jumlah mililiter air terlebih dahulu.");
                return;
            }
            int ml = Integer.parseInt(txt.trim());
            String catatan = txtCatatanHidrasi.getText();
            service.tambahHidrasi(penggunaAktif.getId(), tanggalDipilih, ml, catatan);
            txtCustomHidrasi.clear();
            txtCatatanHidrasi.clear();
            muatHidrasi();
        } catch (NumberFormatException e) {
            DialogHelper.showError("Input Tidak Valid", "Jumlah air harus berupa angka bulat (misal 300).");
        } catch (Exception e) {
            DialogHelper.showError("Error", e.getMessage());
        }
    }

    // --- HANDLER AKTIVITAS ---
    @FXML
    public void handleSimpanAktivitas() {
        try {
            String jenis = comboPresetAktivitas.getValue();
            if (jenis == null || jenis.trim().isEmpty()) {
                DialogHelper.showWarning("Pilihan Kosong", "Pilih jenis aktivitas terlebih dahulu.");
                return;
            }
            String durasiStr = txtDurasiAktivitas.getText();
            if (durasiStr == null || durasiStr.trim().isEmpty()) {
                DialogHelper.showWarning("Input Kurang", "Masukkan durasi aktivitas dalam menit.");
                return;
            }
            int durasi = Integer.parseInt(durasiStr.trim());
            String ket = txtKeteranganAktivitas.getText();

            service.tambahAktivitas(penggunaAktif.getId(), tanggalDipilih, jenis, durasi, ket);
            txtDurasiAktivitas.clear();
            txtKeteranganAktivitas.clear();
            muatAktivitas();
            DialogHelper.showSuccess("Tersimpan", "Catatan aktivitas fisik berhasil disimpan!");
        } catch (NumberFormatException e) {
            DialogHelper.showError("Input Tidak Valid", "Durasi aktivitas harus berupa angka menit (misal 20).");
        } catch (Exception e) {
            DialogHelper.showError("Error", e.getMessage());
        }
    }

    // --- HANDLER MAKAN ---
    @FXML
    public void handleSimpanMakan() {
        try {
            String waktu = comboWaktuMakan.getValue();
            String dbWaktu = "sarapan";
            if ("Makan Siang".equalsIgnoreCase(waktu)) dbWaktu = "makan_siang";
            else if ("Makan Malam".equalsIgnoreCase(waktu)) dbWaktu = "makan_malam";
            else if ("Camilan Sehat".equalsIgnoreCase(waktu)) dbWaktu = "camilan";

            String menu = txtNamaMenu.getText();
            String ket = txtKeteranganMakan.getText();

            service.tambahMakan(penggunaAktif.getId(), tanggalDipilih, dbWaktu, menu, ket);
            txtNamaMenu.clear();
            txtKeteranganMakan.clear();
            muatMakan();
            DialogHelper.showSuccess("Tersimpan", "Catatan pola makan berhasil disimpan!");
        } catch (Exception e) {
            DialogHelper.showError("Error", e.getMessage());
        }
    }

    @FXML
    public void handleUbahTanggal() {
        if (datePickerTanggal.getValue() != null) {
            tanggalDipilih = datePickerTanggal.getValue();
            muatSemuaData();
        }
    }

    @FXML
    public void handlePilihHariIni() {
        tanggalDipilih = LocalDate.now();
        datePickerTanggal.setValue(tanggalDipilih);
        muatSemuaData();
    }
}
