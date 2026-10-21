package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.Rekomendasi;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.RekomendasiService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DialogHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RekomendasiController {

    @FXML private VBox rekomendasiRoot;
    @FXML private Label labelJumlahRekomendasi;

    @FXML private ComboBox<String> comboWaktuLuang;
    @FXML private Label labelWaktuLuangInfo;
    @FXML private ComboBox<String> comboKesibukan;
    @FXML private ComboBox<String> comboUsia;

    @FXML private Button btnTipeSemua;
    @FXML private Button btnTipeAktivitas;
    @FXML private Button btnTipeMenu;
    @FXML private Button btnTipeWorkout;

    @FXML private VBox cardsContainer;
    @FXML private VBox emptyStateBox;
    @FXML private ImageView emptyStateImageView;

    private final RekomendasiService service = new RekomendasiService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;

    private String tipeDipilih = "semua";

    @FXML
    public void initialize() {
        inisialisasiPilihanFilter();
        muatDataPengguna();
        muatRekomendasi();
        AnimationHelper.fadeIn(rekomendasiRoot, 350);
    }

    private void inisialisasiPilihanFilter() {
        comboWaktuLuang.setItems(FXCollections.observableArrayList(
            "Semua Durasi",
            "5 Menit (Cepat)",
            "10 Menit",
            "15 Menit",
            "20 Menit",
            "30 Menit",
            "45 Menit",
            "60 Menit"
        ));
        comboWaktuLuang.setValue("30 Menit");

        comboKesibukan.setItems(FXCollections.observableArrayList(
            "Semua Kesibukan",
            "Pekerja Luar Rumah",
            "Pekerja Dari Rumah / Domestik"
        ));
        comboKesibukan.setValue("Semua Kesibukan");

        comboUsia.setItems(FXCollections.observableArrayList(
            "Semua Usia",
            "Dewasa",
            "Lansia"
        ));
        comboUsia.setValue("Semua Usia");

        try {
            var imgUrl = getClass().getResource("/images/illustration_food.png");
            if (imgUrl != null) {
                emptyStateImageView.setImage(new Image(imgUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}
    }

    private void muatDataPengguna() {
        try {
            penggunaAktif = penggunaRepo.getDefaultPengguna();
        } catch (SQLException ignored) {}
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }

        // Set default filter sesuai profil pengguna
        if ("dalam_rumah".equalsIgnoreCase(penggunaAktif.getGolonganKesibukan())) {
            comboKesibukan.setValue("Pekerja Dari Rumah / Domestik");
        } else {
            comboKesibukan.setValue("Pekerja Luar Rumah");
        }

        if ("lansia".equalsIgnoreCase(penggunaAktif.getGolonganUsia())) {
            comboUsia.setValue("Lansia");
        } else {
            comboUsia.setValue("Dewasa");
        }

        int waktu = penggunaAktif.getWaktuLuangMenit();
        if (waktu <= 5) comboWaktuLuang.setValue("5 Menit (Cepat)");
        else if (waktu <= 10) comboWaktuLuang.setValue("10 Menit");
        else if (waktu <= 15) comboWaktuLuang.setValue("15 Menit");
        else if (waktu <= 20) comboWaktuLuang.setValue("20 Menit");
        else if (waktu <= 30) comboWaktuLuang.setValue("30 Menit");
        else comboWaktuLuang.setValue("45 Menit");
    }

    @FXML
    public void handleFilterChanged() {
        muatRekomendasi();
    }

    @FXML
    public void handleResetFilter() {
        comboWaktuLuang.setValue("Semua Durasi");
        comboKesibukan.setValue("Semua Kesibukan");
        comboUsia.setValue("Semua Usia");
        tipeDipilih = "semua";
        updateTipeButtonStyles();
        muatRekomendasi();
    }

    public void muatRekomendasi() {
        try {
            int maxDurasi = parseWaktuLuang(comboWaktuLuang.getValue());
            if (maxDurasi > 0) {
                labelWaktuLuangInfo.setText("Aktivitas dijamin ≤ " + maxDurasi + " menit");
            } else {
                labelWaktuLuangInfo.setText("Semua durasi aktivitas ditampilkan");
            }

            String kesibukan = parseKesibukan(comboKesibukan.getValue());
            String usia = parseUsia(comboUsia.getValue());

            List<Rekomendasi> list = service.getRekomendasiMenuDanWorkout(tipeDipilih, kesibukan, usia);

            // Saring durasi jika tipe aktivitas atau workout dan ada batasan waktu luang (F-02)
            if (maxDurasi > 0) {
                list = list.stream()
                    .filter(r -> "menu".equalsIgnoreCase(r.getTipe()) || r.getDurasiMenit() <= maxDurasi)
                    .toList();
            }

            if (labelJumlahRekomendasi != null) {
                labelJumlahRekomendasi.setText(list.size() + " Rekomendasi Tersedia");
            }

            renderCards(list);
        } catch (SQLException e) {
            DialogHelper.showError("Kendala Memuat Data", "Gagal memuat katalog rekomendasi: " + e.getMessage());
        }
    }

    private void renderCards(List<Rekomendasi> list) {
        cardsContainer.getChildren().clear();

        if (list.isEmpty()) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            return;
        } else {
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
        }

        for (Rekomendasi r : list) {
            VBox card = new VBox(12);
            card.getStyleClass().add("wellness-card");

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            // Badge Tipe
            Label badgeTipe = new Label(r.getLabelTipe().toUpperCase());
            if ("menu".equalsIgnoreCase(r.getTipe())) {
                badgeTipe.setStyle("-fx-background-color: #FEF3EB; -fx-text-fill: #995200; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
            } else if ("workout".equalsIgnoreCase(r.getTipe())) {
                badgeTipe.setStyle("-fx-background-color: #EBF3FD; -fx-text-fill: #1C5199; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
            } else {
                badgeTipe.setStyle("-fx-background-color: #EEF4F1; -fx-text-fill: #274337; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
            }

            // Badge Durasi tanpa emoji
            Label badgeDurasi = new Label(r.getDurasiMenit() > 0 ? r.getDurasiMenit() + " Menit" : "Fleksibel");
            badgeDurasi.getStyleClass().add("badge-category");

            // Sasaran Demografis
            String sasaranText = ("semua".equalsIgnoreCase(r.getGolonganUsia()) ? "Semua Usia" : ("lansia".equalsIgnoreCase(r.getGolonganUsia()) ? "Lansia" : "Dewasa")) + " • " +
                                 ("semua".equalsIgnoreCase(r.getGolonganKesibukan()) ? "Semua Rutinitas" : ("dalam_rumah".equalsIgnoreCase(r.getGolonganKesibukan()) ? "Di Dalam Rumah" : "Di Luar Rumah"));
            Label badgeSasaran = new Label(sasaranText);
            badgeSasaran.setStyle("-fx-font-size: 12px; -fx-text-fill: #4D6157;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badgeKesesuaian = new Label("Sesuai Waktu Luang");
            badgeKesesuaian.setStyle("-fx-font-size: 11px; -fx-text-fill: #2D6B50; -fx-font-weight: 600;");

            topRow.getChildren().addAll(badgeTipe, badgeDurasi, badgeSasaran, spacer, badgeKesesuaian);

            Label lblJudul = new Label(r.getJudul());
            lblJudul.getStyleClass().add("card-title");
            lblJudul.setStyle("-fx-font-size: 16.5px; -fx-font-weight: bold; -fx-text-fill: #1E3129;");

            Label lblDesc = new Label(r.getDeskripsi());
            lblDesc.getStyleClass().add("card-subtitle");
            lblDesc.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #3E5248; -fx-line-spacing: 3px;");
            lblDesc.setWrapText(true);

            // Bottom Contextual guidance
            HBox bottomRow = new HBox(8);
            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.setStyle("-fx-padding: 6 0 0 0;");

            String guidanceText;
            if ("menu".equalsIgnoreCase(r.getTipe())) {
                guidanceText = "Gizi Seimbang: Mendukung pemenuhan serat, vitamin alami, dan hidrasi harian.";
            } else if ("workout".equalsIgnoreCase(r.getTipe())) {
                guidanceText = "Latihan Terarah: Ramah persendian, tanpa membutuhkan peralatan rumit.";
            } else {
                guidanceText = "Aktivitas Harian: Membantu menjaga metabolisme tubuh tetap aktif di sela kesibukan.";
            }

            Label lblGuidance = new Label(guidanceText);
            lblGuidance.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #5A6D63;");

            bottomRow.getChildren().add(lblGuidance);

            card.getChildren().addAll(topRow, lblJudul, lblDesc, bottomRow);
            cardsContainer.getChildren().add(card);
        }
    }

    private int parseWaktuLuang(String val) {
        if (val == null || val.startsWith("Semua")) return 0;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String parseKesibukan(String val) {
        if (val == null || val.startsWith("Semua")) return "semua";
        if (val.contains("Dari Rumah")) return "dalam_rumah";
        return "luar_rumah";
    }

    private String parseUsia(String val) {
        if (val == null || val.startsWith("Semua")) return "semua";
        if (val.equalsIgnoreCase("Lansia")) return "lansia";
        return "dewasa";
    }

    private void updateTipeButtonStyles() {
        btnTipeSemua.getStyleClass().setAll("btn-outline");
        btnTipeAktivitas.getStyleClass().setAll("btn-outline");
        btnTipeMenu.getStyleClass().setAll("btn-outline");
        btnTipeWorkout.getStyleClass().setAll("btn-outline");

        if ("semua".equals(tipeDipilih)) btnTipeSemua.getStyleClass().setAll("btn-primary");
        else if ("aktivitas".equals(tipeDipilih)) btnTipeAktivitas.getStyleClass().setAll("btn-primary");
        else if ("menu".equals(tipeDipilih)) btnTipeMenu.getStyleClass().setAll("btn-primary");
        else if ("workout".equals(tipeDipilih)) btnTipeWorkout.getStyleClass().setAll("btn-primary");
    }

    @FXML public void handleFilterTipeSemua() { tipeDipilih = "semua"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeAktivitas() { tipeDipilih = "aktivitas"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeMenu() { tipeDipilih = "menu"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeWorkout() { tipeDipilih = "workout"; updateTipeButtonStyles(); muatRekomendasi(); }
}
