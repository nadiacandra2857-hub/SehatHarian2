package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
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

public class LoginController implements AutoCloseable {
    private boolean saving, released;
    private javafx.animation.PauseTransition loginPulse;
    @FXML private Label errIdentity, errPassword;
    private final AuthService authService;
    private final SceneNavigator navigator;

    public LoginController(AuthService authService, SceneNavigator navigator) {
        this.authService = java.util.Objects.requireNonNull(authService);
        this.navigator = java.util.Objects.requireNonNull(navigator);
    }


    @FXML private ImageView brandLogoView;
    @FXML private javafx.scene.control.ScrollPane authRoot;
    @FXML private javafx.scene.layout.VBox authHeroPane;
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
        com.sehatharian.util.AuthLayoutHelper.bindResponsiveHero(authRoot, authHeroPane);
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
        if (saving || released) return;
        hideError();
        inline(errIdentity, ""); inline(errPassword, "");

        String identity = (txtUsername != null && txtUsername.getText() != null) ? txtUsername.getText().trim() : "";
        String password = (txtPassword != null && txtPassword.getText() != null) ? txtPassword.getText() : "";

        // Validasi input kosong
        if (identity.isEmpty()) {
            inline(errIdentity, "Silakan masukkan alamat email Anda.");
            if (txtUsername != null) txtUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            inline(errPassword, "Silakan masukkan kata sandi Anda.");
            if (txtPassword != null) txtPassword.requestFocus();
            return;
        }

        // Feedback visual tombol
        if (btnLogin != null) {
            AnimationHelper.scaleButtonFeedback(btnLogin);
        }

        saving = true; btnLogin.setDisable(true); btnLogin.setText("Memeriksa akun…");
        loginPulse = new javafx.animation.PauseTransition(javafx.util.Duration.millis(80));
        loginPulse.setOnFinished(event -> { if (!released) performLogin(identity, password); });
        loginPulse.play();
    }
    private void performLogin(String identity, String password) {
        try {
            Pengguna user = authService.login(identity, password);
            if (user == null) {
                showError(AuthService.INVALID_CREDENTIALS);
                return;
            }

            // Arahkan ke Onboarding jika belum selesai, atau langsung ke Dashboard jika sudah
            if (user.isOnboardingSelesai()) {
                navigator.showMainApp();
            } else {
                navigator.showOnboarding();
            }
        } catch (SecurityException e) {
            showError(AuthService.INVALID_CREDENTIALS);
        } catch (java.sql.SQLException e) {
            showError("Layanan akun belum tersedia. Silakan coba lagi.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Tidak dapat masuk saat ini. Silakan coba lagi.");
        } finally { saving=false; if(!released) {btnLogin.setDisable(false);btnLogin.setText("Masuk ke Akun");} }
    }

    @FXML
    public void handleForgotPassword() {
        DialogHelper.showInfo(authRoot, "Lupa Kata Sandi",
                "Fitur reset kata sandi belum terhubung ke server autentikasi eksternal.\n\n" +
                "Gunakan kredensial akun yang terdaftar. Hubungi pengelola jika memerlukan bantuan pemulihan akun.");
    }

    @FXML
    public void handleGoToCreateAccount() {
        navigator.showCreateAccount();
    }

    @FXML
    public void handleBackToLanding() {
        navigator.showLandingPage();
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

    private void inline(Label label,String text) {
        if(label==null)return;label.setText(text);label.setVisible(!text.isEmpty());label.setManaged(!text.isEmpty());
    }
    @Override public void close() { released=true;if(loginPulse!=null)loginPulse.stop(); }
    private void hideError() {
        if (errorBannerBox != null) {
            errorBannerBox.setVisible(false);
            errorBannerBox.setManaged(false);
        }
    }
}
