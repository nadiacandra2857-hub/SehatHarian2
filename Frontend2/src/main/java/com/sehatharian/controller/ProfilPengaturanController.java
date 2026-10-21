package com.sehatharian.controller;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Pengguna;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DialogHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;

public class ProfilPengaturanController {

    @FXML private VBox profilRoot;
    @FXML private TextField txtNama;
    @FXML private DatePicker datePickerTanggalLahir;
    @FXML private TextField txtWaktuLuang;
    @FXML private ComboBox<String> comboGolonganUsia;
    @FXML private ComboBox<String> comboGolonganKesibukan;

    @FXML private Label labelJdbcUrl;
    @FXML private Label labelDbStatus;

    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;
    private MainLayoutController mainController;

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        comboGolonganUsia.setItems(FXCollections.observableArrayList(
            "Dewasa (18 - 59 Tahun)",
            "Lansia (≥ 60 Tahun)"
        ));

        comboGolonganKesibukan.setItems(FXCollections.observableArrayList(
            "Pekerja di Luar Rumah (Mobilitas/Kantor)",
            "Pekerja di Dalam Rumah (WFH / Domestik)"
        ));

        labelJdbcUrl.setText(DatabaseConfig.getJdbcUrl());
        handleTestConnection();

        muatDataPengguna();
        AnimationHelper.fadeIn(profilRoot, 350);
    }

    private void muatDataPengguna() {
        try {
            penggunaAktif = penggunaRepo.getDefaultPengguna();
        } catch (SQLException e) {
            System.err.println("Gagal memuat profil pengguna dari DB: " + e.getMessage());
        }
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia Ayu Candra", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }

        txtNama.setText(penggunaAktif.getNama());
        if (penggunaAktif.getTanggalLahir() != null) {
            datePickerTanggalLahir.setValue(penggunaAktif.getTanggalLahir());
        }
        txtWaktuLuang.setText(String.valueOf(penggunaAktif.getWaktuLuangMenit()));

        if ("lansia".equalsIgnoreCase(penggunaAktif.getGolonganUsia())) {
            comboGolonganUsia.setValue("Lansia (≥ 60 Tahun)");
        } else {
            comboGolonganUsia.setValue("Dewasa (18 - 59 Tahun)");
        }

        if ("dalam_rumah".equalsIgnoreCase(penggunaAktif.getGolonganKesibukan())) {
            comboGolonganKesibukan.setValue("Pekerja di Dalam Rumah (WFH / Domestik)");
        } else {
            comboGolonganKesibukan.setValue("Pekerja di Luar Rumah (Mobilitas/Kantor)");
        }
    }

    @FXML
    public void handleSimpanProfil() {
        try {
            String nama = txtNama.getText();
            if (nama == null || nama.trim().isEmpty()) {
                DialogHelper.showWarning("Input Kurang", "Nama lengkap wajib diisi.");
                return;
            }

            String waktuLuangStr = txtWaktuLuang.getText();
            int waktuLuang = 30;
            if (waktuLuangStr != null && !waktuLuangStr.trim().isEmpty()) {
                try {
                    waktuLuang = Integer.parseInt(waktuLuangStr.trim());
                    if (waktuLuang <= 0 || waktuLuang > 480) {
                        DialogHelper.showWarning("Input Tidak Valid", "Waktu luang harus antara 1 sampai 480 menit.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    DialogHelper.showError("Format Salah", "Waktu luang harus berupa angka menit.");
                    return;
                }
            }

            String usia = comboGolonganUsia.getValue() != null && comboGolonganUsia.getValue().contains("Lansia") ? "lansia" : "dewasa";
            String kesibukan = comboGolonganKesibukan.getValue() != null && comboGolonganKesibukan.getValue().contains("Dalam Rumah") ? "dalam_rumah" : "luar_rumah";

            penggunaAktif.setNama(nama.trim());
            penggunaAktif.setTanggalLahir(datePickerTanggalLahir.getValue());
            penggunaAktif.setWaktuLuangMenit(waktuLuang);
            penggunaAktif.setGolonganUsia(usia);
            penggunaAktif.setGolonganKesibukan(kesibukan);

            penggunaRepo.updatePengguna(penggunaAktif);

            if (mainController != null) {
                mainController.updateUserInfoSidebar(penggunaAktif);
            }

            DialogHelper.showSuccess("Sukses", "Profil pengguna berhasil diperbarui di MySQL!");
        } catch (SQLException e) {
            DialogHelper.showError("Database Error", "Gagal menyimpan perubahan profil: " + e.getMessage());
        }
    }

    @FXML
    public void handleMuatUlang() {
        muatDataPengguna();
    }

    @FXML
    public void handleTestConnection() {
        boolean ok = DatabaseConfig.testConnection();
        if (ok) {
            labelDbStatus.setText("✓ Terhubung (MySQL 8.0)");
            labelDbStatus.getStyleClass().setAll("badge-selesai");
        } else {
            labelDbStatus.setText("✕ Terputus / Gagal Terhubung");
            labelDbStatus.getStyleClass().setAll("badge-warning");
        }
    }
}
