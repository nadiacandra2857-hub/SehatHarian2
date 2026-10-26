package com.sehatharian.controller;

import com.sehatharian.model.UserSession;
import com.sehatharian.service.PenggunaService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class OnboardingController {

    // Progress Header
    @FXML private Label step1Badge;
    @FXML private Label step2Badge;
    @FXML private Label step3Badge;
    @FXML private StackPane progressFillPane;

    // Slide Containers
    @FXML private VBox slide1Container;
    @FXML private VBox slide2Container;
    @FXML private VBox slide3Container;

    // Slide 1 Options (Waktu Luang, maks 4 jam)
    @FXML private HBox cardWaktu0;
    @FXML private HBox cardWaktu1;
    @FXML private HBox cardWaktu2;
    @FXML private HBox cardWaktu3;
    @FXML private HBox cardWaktu4;

    // Slide 2 Options (Aktivitas Outdoor / Indoor)
    @FXML private VBox cardOutdoor;
    @FXML private VBox cardIndoor;
    @FXML private ImageView imgOutdoorView;
    @FXML private ImageView imgIndoorView;

    // Slide 3 Options (Kelompok Umur)
    @FXML private VBox cardRemaja;
    @FXML private VBox cardDewasa;
    @FXML private VBox cardLansia;

    // Navigation & Validation
    @FXML private Label validationMessageLabel;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button btnFinish;

    // State Variables (Tersimpan saat bolak-balik slide)
    private int currentSlide = 1;
    private Integer selectedWaktuLuang = null; // 0 (<1 jam), 1, 2, 3, 4
    private String selectedWaktuLuangLabel = null;
    private String selectedJenisAktivitas = null; // "OUTDOOR" atau "INDOOR"
    private String selectedKelompokUmur = null; // "REMAJA", "DEWASA", atau "LANSIA"

    @FXML
    public void initialize() {
        muatGambarSlide2();
        updateTampilanSlide();
    }

    private void muatGambarSlide2() {
        try {
            var walkUrl = getClass().getResource("/images/illustration_walk.png");
            if (walkUrl != null) imgOutdoorView.setImage(new Image(walkUrl.toExternalForm()));

            var stretchUrl = getClass().getResource("/images/illustration_stretch.png");
            if (stretchUrl != null) imgIndoorView.setImage(new Image(stretchUrl.toExternalForm()));
        } catch (Exception ignored) {}
    }

    // --- SELECTION HANDLERS: SLIDE 1 (WAKTU LUANG) ---

    @FXML public void handlePilihWaktu0() { setWaktuLuang(0, "Kurang dari 1 jam (30-45 menit)"); }
    @FXML public void handlePilihWaktu1() { setWaktuLuang(1, "1 jam"); }
    @FXML public void handlePilihWaktu2() { setWaktuLuang(2, "2 jam"); }
    @FXML public void handlePilihWaktu3() { setWaktuLuang(3, "3 jam"); }
    @FXML public void handlePilihWaktu4() { setWaktuLuang(4, "4 jam"); }

    private void setWaktuLuang(int hours, String label) {
        this.selectedWaktuLuang = hours;
        this.selectedWaktuLuangLabel = label;
        hideValidationMessage();

        // Update CSS selected class
        HBox[] cards = {cardWaktu0, cardWaktu1, cardWaktu2, cardWaktu3, cardWaktu4};
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].getStyleClass().remove("selected");
                if (i == hours) {
                    cards[i].getStyleClass().add("selected");
                    AnimationHelper.pulseSuccess(cards[i]);
                }
            }
        }
        btnNext.setDisable(false);
    }

    // --- SELECTION HANDLERS: SLIDE 2 (AKTIVITAS OUTDOOR / INDOOR) ---

    @FXML
    public void handlePilihOutdoor() {
        setJenisAktivitas("OUTDOOR");
    }

    @FXML
    public void handlePilihIndoor() {
        setJenisAktivitas("INDOOR");
    }

    private void setJenisAktivitas(String jenis) {
        this.selectedJenisAktivitas = jenis;
        hideValidationMessage();

        cardOutdoor.getStyleClass().remove("selected");
        cardIndoor.getStyleClass().remove("selected");

        if ("OUTDOOR".equals(jenis)) {
            cardOutdoor.getStyleClass().add("selected");
            AnimationHelper.pulseSuccess(cardOutdoor);
        } else {
            cardIndoor.getStyleClass().add("selected");
            AnimationHelper.pulseSuccess(cardIndoor);
        }
        btnNext.setDisable(false);
    }

    // --- SELECTION HANDLERS: SLIDE 3 (KELOMPOK UMUR) ---

    @FXML public void handlePilihRemaja() { setKelompokUmur("REMAJA"); }
    @FXML public void handlePilihDewasa() { setKelompokUmur("DEWASA"); }
    @FXML public void handlePilihLansia() { setKelompokUmur("LANSIA"); }

    private void setKelompokUmur(String umur) {
        this.selectedKelompokUmur = umur;
        hideValidationMessage();

        cardRemaja.getStyleClass().remove("selected");
        cardDewasa.getStyleClass().remove("selected");
        cardLansia.getStyleClass().remove("selected");

        if ("REMAJA".equals(umur)) {
            cardRemaja.getStyleClass().add("selected");
            AnimationHelper.pulseSuccess(cardRemaja);
        } else if ("DEWASA".equals(umur)) {
            cardDewasa.getStyleClass().add("selected");
            AnimationHelper.pulseSuccess(cardDewasa);
        } else {
            cardLansia.getStyleClass().add("selected");
            AnimationHelper.pulseSuccess(cardLansia);
        }
        btnFinish.setDisable(false);
    }

    // --- NAVIGATION FLOW & VALIDATION ---

    @FXML
    public void handleNext() {
        if (currentSlide == 1) {
            if (selectedWaktuLuang == null) {
                showValidationMessage("Silakan pilih salah satu jawaban waktu luang untuk melanjutkan.");
                return;
            }
            currentSlide = 2;
            updateTampilanSlide();
        } else if (currentSlide == 2) {
            if (selectedJenisAktivitas == null) {
                showValidationMessage("Silakan pilih preferensi aktivitas (Outdoor atau Indoor) untuk melanjutkan.");
                return;
            }
            currentSlide = 3;
            updateTampilanSlide();
        }
    }

    @FXML
    public void handlePrev() {
        hideValidationMessage();
        if (currentSlide > 1) {
            currentSlide--;
            updateTampilanSlide();
        }
    }

    @FXML
    public void handleFinish() {
        // Validasi kelengkapan semua 3 slide sebelum menyelesaikan
        if (selectedWaktuLuang == null || selectedJenisAktivitas == null || selectedKelompokUmur == null) {
            showValidationMessage("Silakan pastikan semua pertanyaan telah dijawab sebelum menyelesaikan onboarding.");
            return;
        }

        AnimationHelper.scaleButtonFeedback(btnFinish);

        String waktuLabel = selectedWaktuLuangLabel != null ? selectedWaktuLuangLabel : (selectedWaktuLuang + " jam");

        // Simpan jawaban ke UserSession
        UserSession session = UserSession.getInstance();
        session.completeOnboarding(
                selectedWaktuLuang,
                waktuLabel,
                selectedJenisAktivitas,
                selectedKelompokUmur
        );

        // Persist ke database melalui PenggunaService
        try {
            PenggunaService penggunaService = new PenggunaService();
            int userId = session.getUserId() > 0 ? session.getUserId() : 1;
            penggunaService.simpanOnboarding(
                    userId,
                    selectedWaktuLuang,
                    waktuLabel,
                    selectedJenisAktivitas,
                    selectedKelompokUmur
            );
        } catch (Exception e) {
            System.err.println("Gagal menyimpan onboarding ke database: " + e.getMessage());
        }

        // Arahkan ke Dashboard
        SceneNavigator.showMainApp();
    }

    private void updateTampilanSlide() {
        hideValidationMessage();

        // 1. Tampilkan kontainer slide yang aktif
        slide1Container.setVisible(currentSlide == 1);
        slide1Container.setManaged(currentSlide == 1);

        slide2Container.setVisible(currentSlide == 2);
        slide2Container.setManaged(currentSlide == 2);

        slide3Container.setVisible(currentSlide == 3);
        slide3Container.setManaged(currentSlide == 3);

        // Animasi transisi ringan
        if (currentSlide == 1) AnimationHelper.fadeIn(slide1Container, 250);
        else if (currentSlide == 2) AnimationHelper.fadeIn(slide2Container, 250);
        else if (currentSlide == 3) AnimationHelper.fadeIn(slide3Container, 250);

        // 2. Indikator langkah & progress track
        step1Badge.getStyleClass().remove("active");
        step2Badge.getStyleClass().remove("active");
        step3Badge.getStyleClass().remove("active");

        if (currentSlide == 1) {
            step1Badge.getStyleClass().add("active");
            progressFillPane.setPrefWidth(220.0);
        } else if (currentSlide == 2) {
            step1Badge.getStyleClass().add("active");
            step2Badge.getStyleClass().add("active");
            progressFillPane.setPrefWidth(440.0);
        } else {
            step1Badge.getStyleClass().add("active");
            step2Badge.getStyleClass().add("active");
            step3Badge.getStyleClass().add("active");
            progressFillPane.setPrefWidth(660.0);
        }

        // 3. Status tombol navigasi
        // Slide 1: Tombol kembali disembunyikan/dinonaktifkan
        btnPrev.setVisible(currentSlide > 1);
        btnPrev.setManaged(currentSlide > 1);

        // Slide 1 & 2: Tombol Berikutnya. Slide 3: Tombol Selesai
        btnNext.setVisible(currentSlide < 3);
        btnNext.setManaged(currentSlide < 3);
        btnFinish.setVisible(currentSlide == 3);
        btnFinish.setManaged(currentSlide == 3);

        // Status aktif tombol berdasarkan jawaban yang sudah tersimpan
        if (currentSlide == 1) {
            btnNext.setDisable(selectedWaktuLuang == null);
        } else if (currentSlide == 2) {
            btnNext.setDisable(selectedJenisAktivitas == null);
        } else if (currentSlide == 3) {
            btnFinish.setDisable(selectedKelompokUmur == null);
        }
    }

    private void showValidationMessage(String msg) {
        validationMessageLabel.setText(msg);
        validationMessageLabel.setVisible(true);
        validationMessageLabel.setManaged(true);
        AnimationHelper.pulseSuccess(validationMessageLabel);
    }

    private void hideValidationMessage() {
        validationMessageLabel.setText("");
        validationMessageLabel.setVisible(false);
        validationMessageLabel.setManaged(false);
    }
}
