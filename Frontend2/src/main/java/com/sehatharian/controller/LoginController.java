package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.AuthService;
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
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null && brandLogoView != null) {
                brandLogoView.setImage(new Image(logoUrl.toExternalForm()));
            }
        } catch (Exception ignored) {
            // Abaikan jika berkas gambar logo tidak ditemukan pada runtime
        }

        // Sinkronisasi teks password tersembunyi dan terlihat
        if (txtPasswordVisible != null && txtPassword != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }

        // Default sembunyikan error
        hideError();
    }

    @FXML
    public void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            if (txtPasswordVisible != null) {
                txtPasswordVisible.setVisible(true);
                txtPasswordVisible.setManaged(true);
                txtPasswordVisible.requestFocus();
                txtPasswordVisible.positionCaret(txtPasswordVisible.getText() != null ? txtPasswordVisible.getText().length() : 0);
            }
            if (txtPassword != null) {
                txtPassword.setVisible(false);
                txtPassword.setManaged(false);
            }
            if (btnTogglePassword != null) {
                btnTogglePassword.setText("Sembunyikan");
            }
        } else {
            if (txtPassword != null) {
                txtPassword.setVisible(true);
                txtPassword.setManaged(true);
                txtPassword.requestFocus();
                txtPassword.positionCaret(txtPassword.getText() != null ? txtPassword.getText().length() : 0);
            }
            if (txtPasswordVisible != null) {
                txtPasswordVisible.setVisible(false);
                txtPasswordVisible.setManaged(false);
            }
            if (btnTogglePassword != null) {
                btnTogglePassword.setText("Lihat");
            }
        }
    }

    @FXML
    public void handleLogin() {
        hideError();

        String identity = (txtUsername != null && txtUsername.getText() != null) ? txtUsername.getText().trim() : "";
        String password = (txtPassword != null && txtPassword.getText() != null) ? txtPassword.getText() : "";

        // Validasi input kosong
        if (identity.isEmpty()) {
            showError("Silakan masukkan email atau nama pengguna Anda.");
            if (txtUsername != null) txtUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Silakan masukkan kata sandi Anda.");
            if (txtPassword != null) txtPassword.requestFocus();
            return;
        }

        if (password.length() < 4) {
            showError("Kata sandi minimal 4 karakter.");
            if (txtPassword != null) txtPassword.requestFocus();
            return;
        }

        // Feedback visual tombol
        if (btnLogin != null) {
            AnimationHelper.scaleButtonFeedback(btnLogin);
        }

        try {
            Pengguna user = authService.login(identity, password);
            if (user == null) {
                showError("Email atau kata sandi yang Anda masukkan salah.");
                return;
            }

            boolean rememberMe = chkRememberMe != null && chkRememberMe.isSelected();
            UserSession session = UserSession.getInstance();
            session.setCurrentUser(user);
            if (rememberMe) {
                session.setDemoMode(false);
            }

            // Arahkan ke Onboarding jika belum selesai, atau langsung ke Dashboard jika sudah
            if (user.isOnboardingSelesai()) {
                SceneNavigator.showMainApp();
            } else {
                SceneNavigator.showOnboarding();
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Terjadi kesalahan saat masuk: " + e.getMessage());
        }
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
        if (errorBannerLabel != null) {
            errorBannerLabel.setText(message);
        }
        if (errorBannerBox != null) {
            errorBannerBox.setVisible(true);
            errorBannerBox.setManaged(true);
            AnimationHelper.pulseSuccess(errorBannerBox);
        }
    }

    private void hideError() {
        if (errorBannerBox != null) {
            errorBannerBox.setVisible(false);
            errorBannerBox.setManaged(false);
        }
    }
}
