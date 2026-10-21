package com.sehatharian.controller;

import com.sehatharian.model.UserSession;
import com.sehatharian.util.AnimationHelper;
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

import java.util.regex.Pattern;

public class CreateAccountController {

    @FXML private ImageView brandLogoView;
    @FXML private TextField txtNama;
    @FXML private TextField txtEmail;

    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnTogglePassword;

    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtConfirmPasswordVisible;
    @FXML private Button btnToggleConfirmPassword;

    @FXML private CheckBox chkTerms;

    @FXML private HBox errorBannerBox;
    @FXML private Label errorBannerLabel;

    @FXML private Label errNama;
    @FXML private Label errEmail;
    @FXML private Label errPassword;
    @FXML private Label errConfirmPassword;
    @FXML private Label errTerms;

    @FXML private Button btnRegister;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                brandLogoView.setImage(new Image(logoUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        // Sinkronisasi teks password tersembunyi dan terlihat
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        txtConfirmPasswordVisible.textProperty().bindBidirectional(txtConfirmPassword.textProperty());

        resetErrors();
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
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            btnTogglePassword.setText("Lihat");
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    @FXML
    public void handleToggleConfirmPassword() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            txtConfirmPasswordVisible.setVisible(true);
            txtConfirmPasswordVisible.setManaged(true);
            txtConfirmPassword.setVisible(false);
            txtConfirmPassword.setManaged(false);
            btnToggleConfirmPassword.setText("Sembunyikan");
            txtConfirmPasswordVisible.requestFocus();
            txtConfirmPasswordVisible.positionCaret(txtConfirmPasswordVisible.getText().length());
        } else {
            txtConfirmPassword.setVisible(true);
            txtConfirmPassword.setManaged(true);
            txtConfirmPasswordVisible.setVisible(false);
            txtConfirmPasswordVisible.setManaged(false);
            btnToggleConfirmPassword.setText("Lihat");
            txtConfirmPassword.requestFocus();
            txtConfirmPassword.positionCaret(txtConfirmPassword.getText().length());
        }
    }

    @FXML
    public void handleRegister() {
        resetErrors();

        String nama = txtNama.getText() != null ? txtNama.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";
        String confirmPassword = txtConfirmPassword.getText() != null ? txtConfirmPassword.getText() : "";
        boolean termsChecked = chkTerms.isSelected();

        boolean hasError = false;

        // 1. Validasi Nama
        if (nama.isEmpty()) {
            showInlineError(errNama, "Nama lengkap tidak boleh kosong.");
            hasError = true;
        } else if (nama.length() < 3) {
            showInlineError(errNama, "Nama lengkap minimal 3 karakter.");
            hasError = true;
        }

        // 2. Validasi Email
        if (email.isEmpty()) {
            showInlineError(errEmail, "Alamat email tidak boleh kosong.");
            hasError = true;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            showInlineError(errEmail, "Format email tidak valid (contoh: pengguna@domain.com).");
            hasError = true;
        }

        // 3. Validasi Password
        if (password.isEmpty()) {
            showInlineError(errPassword, "Kata sandi tidak boleh kosong.");
            hasError = true;
        } else if (password.length() < 6) {
            showInlineError(errPassword, "Kata sandi minimal 6 karakter.");
            hasError = true;
        }

        // 4. Validasi Konfirmasi Password
        if (confirmPassword.isEmpty()) {
            showInlineError(errConfirmPassword, "Konfirmasi kata sandi tidak boleh kosong.");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            showInlineError(errConfirmPassword, "Konfirmasi kata sandi tidak cocok dengan kata sandi.");
            hasError = true;
        }

        // 5. Validasi Syarat & Ketentuan
        if (!termsChecked) {
            showInlineError(errTerms, "Anda harus menyetujui syarat & ketentuan untuk melanjutkan.");
            hasError = true;
        }

        if (hasError) {
            showBannerError("Mohon perbaiki isian formulir yang ditandai sebelum melanjutkan.");
            return;
        }

        // Feedback visual tombol diproses
        AnimationHelper.scaleButtonFeedback(btnRegister);

        // Alur Spesifikasi Tahap 6:
        // HANYA setelah pembuatan akun berhasil, pengguna diarahkan ke onboarding slide pertama.
        UserSession session = UserSession.getInstance();
        session.registerNewAccount(nama, email);

        // Arahkan ke Onboarding Slide 1
        SceneNavigator.showOnboarding();
    }

    @FXML
    public void handleGoToLogin() {
        SceneNavigator.showLogin();
    }

    @FXML
    public void handleBackToLanding() {
        SceneNavigator.showLandingPage();
    }

    private void showInlineError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showBannerError(String message) {
        errorBannerLabel.setText(message);
        errorBannerBox.setVisible(true);
        errorBannerBox.setManaged(true);
        AnimationHelper.pulseSuccess(errorBannerBox);
    }

    private void resetErrors() {
        errorBannerBox.setVisible(false);
        errorBannerBox.setManaged(false);

        Label[] inlineLabels = {errNama, errEmail, errPassword, errConfirmPassword, errTerms};
        for (Label lbl : inlineLabels) {
            if (lbl != null) {
                lbl.setText("");
                lbl.setVisible(false);
                lbl.setManaged(false);
            }
        }
    }
}
