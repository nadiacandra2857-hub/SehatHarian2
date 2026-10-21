package com.sehatharian.controller;

import com.sehatharian.util.DialogHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class LandingPageController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private ImageView brandLogoView;
    @FXML private ImageView heroImageView;
    @FXML private ImageView foodIllustrationView;
    @FXML private ImageView sleepIllustrationView;
    @FXML private ImageView walkIllustrationView;
    @FXML private ImageView waterIllustrationView;

    @FXML private Button btnNavBeranda;
    @FXML private Button btnNavFitur;
    @FXML private Button btnNavTentang;
    @FXML private Button btnNavBantuan;

    @FXML private VBox heroSectionBox;
    @FXML private VBox deskripsiSectionBox;
    @FXML private VBox fiturSectionBox;
    @FXML private VBox ctaSectionBox;

    @FXML
    public void initialize() {
        muatGambar();
        setActiveNav(btnNavBeranda);
    }

    private void muatGambar() {
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) brandLogoView.setImage(new Image(logoUrl.toExternalForm()));

            var heroUrl = getClass().getResource("/images/hero_wellness.png");
            if (heroUrl != null) heroImageView.setImage(new Image(heroUrl.toExternalForm()));

            var foodUrl = getClass().getResource("/images/illustration_food.png");
            if (foodUrl != null) foodIllustrationView.setImage(new Image(foodUrl.toExternalForm()));

            var sleepUrl = getClass().getResource("/images/illustration_sleep.png");
            if (sleepUrl != null) sleepIllustrationView.setImage(new Image(sleepUrl.toExternalForm()));

            var walkUrl = getClass().getResource("/images/illustration_walk.png");
            if (walkUrl != null) walkIllustrationView.setImage(new Image(walkUrl.toExternalForm()));

            var waterUrl = getClass().getResource("/images/illustration_water.png");
            if (waterUrl != null) waterIllustrationView.setImage(new Image(waterUrl.toExternalForm()));
        } catch (Exception e) {
            System.err.println("Info muat gambar landing page: " + e.getMessage());
        }
    }

    @FXML
    public void handleNavBeranda() {
        setActiveNav(btnNavBeranda);
        mainScrollPane.setVvalue(0.0);
    }

    @FXML
    public void handleNavFitur() {
        setActiveNav(btnNavFitur);
        mainScrollPane.setVvalue(0.42);
    }

    @FXML
    public void handleNavTentang() {
        setActiveNav(btnNavTentang);
        mainScrollPane.setVvalue(0.20);
    }

    @FXML
    public void handleNavBantuan() {
        setActiveNav(btnNavBantuan);
        DialogHelper.showInfo("Bantuan & Kontak SehatHarian",
                "Aplikasi SehatHarian dirancang untuk membantu membangun kebiasaan hidup sehat secara konsisten.\n\n" +
                "Tim Pengembang: Adhitya Ramadhan, Nadia Ayu Candra Pramana, Muhammad Ai Nur Haaq\n" +
                "Institusi: D3 Teknik Informatika PSDKU Lamongan, PENS 2026\n" +
                "Untuk panduan penggunaan, klik 'Buat Akun' untuk memulai perjalanan hidup sehat Anda!");
    }

    private void setActiveNav(Button activeButton) {
        Button[] buttons = {btnNavBeranda, btnNavFitur, btnNavTentang, btnNavBantuan};
        for (Button b : buttons) {
            if (b != null) b.getStyleClass().remove("active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    public void handleLogin() {
        SceneNavigator.showLogin();
    }

    @FXML
    public void handleCreateAccount() {
        SceneNavigator.showCreateAccount();
    }
}
