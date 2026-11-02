package com.sehatharian.controller;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
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

public class ProfilPengaturanController implements AutoCloseable {
    private final UserSession session;
    private final com.sehatharian.model.SessionAccess access;
    private final com.sehatharian.util.WriteAction writes;

    public ProfilPengaturanController(UserSession session, com.sehatharian.service.PenggunaService profiles) {
        this.session = java.util.Objects.requireNonNull(session);
        this.access = new com.sehatharian.model.SessionAccess(session);
        this.writes = new com.sehatharian.util.WriteAction(access);
        this.profiles = java.util.Objects.requireNonNull(profiles).forView(access);
    }


    @FXML private VBox profilRoot;
    @FXML private TextField txtNama;
    @FXML private DatePicker datePickerTanggalLahir;
    @FXML private TextField txtWaktuLuang;
    @FXML private ComboBox<String> comboGolonganUsia;
    @FXML private ComboBox<String> comboGolonganKesibukan;

    @FXML private Label labelJdbcUrl;
    @FXML private Label labelDbStatus;

    private final com.sehatharian.service.PenggunaService profiles;
    private Pengguna penggunaAktif;
    private MainLayoutController mainController;

    public void setMainController(MainLayoutController mainController) {
        access.owner();
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        access.owner();
        comboGolonganUsia.setItems(FXCollections.observableArrayList(
            "Dewasa (18 - 59 Tahun)",
            "Lansia (≥ 60 Tahun)"
        ));

        comboGolonganKesibukan.setItems(FXCollections.observableArrayList(
            "Di Luar Rumah",
            "Di Dalam Rumah"
        ));

        labelJdbcUrl.setText(DatabaseConfig.getJdbcUrl());
        handleTestConnection();

        muatDataPengguna();
        AnimationHelper.fadeIn(profilRoot, 350);
    }

    private void muatDataPengguna() {
        penggunaAktif = session.requireActiveUser();

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
            comboGolonganKesibukan.setValue("Di Dalam Rumah");
        } else {
            comboGolonganKesibukan.setValue("Di Luar Rumah");
        }
    }

    @FXML
    public void handleSimpanProfil() {
        access.owner();
        try {
            String nama = txtNama.getText();
            if (nama == null || nama.trim().isEmpty()) {
                DialogHelper.showWarning(profilRoot, "Input Kurang", "Nama lengkap wajib diisi.");
                return;
            }

            String waktuLuangStr = txtWaktuLuang.getText();
            int waktuLuang = 30;
            if (waktuLuangStr != null && !waktuLuangStr.trim().isEmpty()) {
                try {
                    waktuLuang = Integer.parseInt(waktuLuangStr.trim());
                    if (waktuLuang <= 0 || waktuLuang > 480) {
                        DialogHelper.showWarning(profilRoot, "Input Tidak Valid", "Waktu luang harus antara 1 sampai 480 menit.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    DialogHelper.showError(profilRoot, "Format Salah", "Waktu luang harus berupa angka menit.");
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

            final int waktuPilihan = waktuLuang;
            writes.submit(profilRoot,
                () -> profiles.updateProfil(penggunaAktif.getId(), nama, datePickerTanggalLahir.getValue(), waktuPilihan, usia, kesibukan),
                () -> {
            access.owner();
            penggunaAktif = access.profile();

            if (mainController != null) {
                mainController.updateUserInfoSidebar(penggunaAktif);
            }

            DialogHelper.showSuccess(profilRoot, "Sukses", "Profil pengguna berhasil diperbarui!");
                });
        } catch (SQLException e) {
            access.owner();
            DialogHelper.showError(profilRoot, "Database Error", "Layanan penyimpanan belum tersedia. Data belum dikonfirmasi; silakan coba lagi.");
        }
    }

    @FXML public void handleLogout() { access.owner(); if(mainController!=null) mainController.handleLogout(); }

    @FXML
    public void handleMuatUlang() {
        access.owner();
        try {
            if (profiles.muatProfilTersimpan() == null) {
                DialogHelper.showWarning(profilRoot, "Profil Belum Tersedia", "Profil tersimpan belum dapat ditemukan.");
                return;
            }
            muatDataPengguna();
        } catch (SQLException e) {
            access.owner();
            DialogHelper.showWarning(profilRoot, "Belum Dapat Memuat", "Profil belum dapat dibaca. Jangan langsung mengulang penyimpanan yang belum pasti.");
        }
    }

    @FXML
    public void handleTestConnection() {
        access.owner();
        boolean ok = DatabaseConfig.testConnection();
        if (ok) {
            labelDbStatus.setText("✓ Terhubung (MySQL 8.0)");
            labelDbStatus.getStyleClass().setAll("badge-selesai");
        } else {
            labelDbStatus.setText("✕ Terputus / Gagal Terhubung");
            labelDbStatus.getStyleClass().setAll("badge-warning");
        }
    }
    @Override public void close() { access.close(); }
}
