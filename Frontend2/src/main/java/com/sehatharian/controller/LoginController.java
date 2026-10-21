package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DialogHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class LoginController {

    @FXML private ImageView brandLogoView;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnTogglePassword;
    @FXML private CheckBox chkRememberMe;
    @FXML private HBox errorBannerBox;
    @FXML private Label errorBannerLabel;
    @FXML private Button btnLogin;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                brandLogoView.setImage(new Image(logoUrl.toExternalForm()));
            }
        } catch (Exception ignored) {
            // Abaikan jika berkas gambar logo tidak ditemukan pada runtime
        }

        // Sinkronisasi teks password tersembunyi dan terlihat
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        // Default sembunyikan error
        hideError();
    }

    @FXML
    public void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            btnTogglePassword.setText("Sembunyikan");
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText() != null ? txtPasswordVisible.getText().length() : 0);
        } else {
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            btnTogglePassword.setText("Lihat");
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText() != null ? txtPassword.getText().length() : 0);
        }
    }

    @FXML
    public void handleLogin() {
        hideError();

        String identity = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        // Validasi input kosong
        if (identity.isEmpty()) {
            showError("Silakan masukkan email atau nama pengguna Anda.");
            txtUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Silakan masukkan kata sandi Anda.");
            txtPassword.requestFocus();
            return;
        }

        if (password.length() < 4) {
            showError("Kata sandi minimal 4 karakter.");
            txtPassword.requestFocus();
            return;
        }

        // Feedback visual tombol
        AnimationHelper.scaleButtonFeedback(btnLogin);

        // Simulasi proses login frontend terisolasi
        // Periksa apakah ada pengguna yang tersimpan di MySQL
        String namaTampilan = identity;
        try {
            PenggunaRepository repo = new PenggunaRepository();
            Pengguna p = repo.getDefaultPengguna();
            if (p != null && p.getNama() != null && !p.getNama().trim().isEmpty()) {
                namaTampilan = p.getNama();
            }
        } catch (Exception ignored) {
            // Abaikan jika database lokal belum aktif, gunakan nama identitas default
        }

        // Periksa opsi Ingat Saya
        boolean rememberMe = chkRememberMe != null && chkRememberMe.isSelected();

        // Inisialisasi sesi login pengguna
        UserSession session = UserSession.getInstance();
        session.login(identity, namaTampilan);
        if (rememberMe) {
            session.setDemoMode(true);
        }

        // Login pengguna yang sudah ada menandai onboarding sudah selesai
        // agar tidak memunculkan onboarding ulang sesuai PRD Ketentuan 5
        session.setOnboardingSelesai(true);

        // Arahkan ke Halaman Utama / Dashboard
        SceneNavigator.showMainApp();
    }

    @FXML
    public void handleForgotPassword() {
        DialogHelper.showInfo("Lupa Kata Sandi",
                "Fitur reset kata sandi belum terhubung ke server autentikasi eksternal.\n\n" +
                "Untuk demonstrasi frontend saat ini, Anda dapat langsung masuk menggunakan akun apa pun atau membuat akun baru di halaman Buat Akun.");
    }

    @FXML
    public void handleGoToCreateAccount() {
        SceneNavigator.showCreateAccount();
    }

    @FXML
    public void handleBackToLanding() {
        SceneNavigator.showLandingPage();
    }

    private void showError(String message) {
        errorBannerLabel.setText(message);
        errorBannerBox.setVisible(true);
        errorBannerBox.setManaged(true);
        AnimationHelper.pulseSuccess(errorBannerBox);
    }

    private void hideError() {
        errorBannerBox.setVisible(false);
        errorBannerBox.setManaged(false);
    }
}
