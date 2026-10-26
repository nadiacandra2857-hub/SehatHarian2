package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.Rekomendasi;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.RekomendasiService;
import com.sehatharian.util.AnimationHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
        if (rekomendasiRoot != null) {
            AnimationHelper.fadeIn(rekomendasiRoot, 350);
        }
    }

    private void inisialisasiPilihanFilter() {
        if (comboWaktuLuang != null) {
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
        }

        if (comboKesibukan != null) {
            comboKesibukan.setItems(FXCollections.observableArrayList(
                "Semua Kesibukan",
                "Pekerja Luar Rumah",
                "Pekerja Dari Rumah / Domestik"
            ));
            comboKesibukan.setValue("Semua Kesibukan");
        }

        if (comboUsia != null) {
            comboUsia.setItems(FXCollections.observableArrayList(
                "Semua Usia",
                "Dewasa",
                "Lansia"
            ));
            comboUsia.setValue("Semua Usia");
        }

        try {
            var imgUrl = getClass().getResource("/images/illustration_food.png");
            if (imgUrl != null && emptyStateImageView != null) {
                emptyStateImageView.setImage(new Image(imgUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}
    }

    private void muatDataPengguna() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.getActiveUser() != null) {
            penggunaAktif = session.getActiveUser();
        } else if (session.isLoggedIn() && session.getUserId() > 0) {
            try {
                penggunaAktif = penggunaRepo.getById(session.getUserId());
            } catch (Exception ignored) {}
        }

        if (penggunaAktif == null) {
            try {
                penggunaAktif = penggunaRepo.getDefaultPengguna();
            } catch (SQLException ignored) {}
        }

        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }

        // Set default filter sesuai profil pengguna
        if (comboKesibukan != null) {
            if ("dalam_rumah".equalsIgnoreCase(penggunaAktif.getGolonganKesibukan())) {
                comboKesibukan.setValue("Pekerja Dari Rumah / Domestik");
            } else if ("semua".equalsIgnoreCase(penggunaAktif.getGolonganKesibukan())) {
                comboKesibukan.setValue("Semua Kesibukan");
            } else {
                comboKesibukan.setValue("Pekerja Luar Rumah");
            }
        }

        if (comboUsia != null) {
            if ("lansia".equalsIgnoreCase(penggunaAktif.getGolonganUsia())) {
                comboUsia.setValue("Lansia");
            } else if ("semua".equalsIgnoreCase(penggunaAktif.getGolonganUsia())) {
                comboUsia.setValue("Semua Usia");
            } else {
                comboUsia.setValue("Dewasa");
            }
        }

        if (comboWaktuLuang != null) {
            int waktu = penggunaAktif.getWaktuLuangMenit();
            if (waktu <= 0) {
                comboWaktuLuang.setValue("Semua Durasi");
            } else if (waktu <= 5) {
                comboWaktuLuang.setValue("5 Menit (Cepat)");
            } else if (waktu <= 10) {
                comboWaktuLuang.setValue("10 Menit");
            } else if (waktu <= 15) {
                comboWaktuLuang.setValue("15 Menit");
            } else if (waktu <= 20) {
                comboWaktuLuang.setValue("20 Menit");
            } else if (waktu <= 30) {
                comboWaktuLuang.setValue("30 Menit");
            } else if (waktu <= 45) {
                comboWaktuLuang.setValue("45 Menit");
            } else {
                comboWaktuLuang.setValue("60 Menit");
            }
        }
    }

    @FXML
    public void handleFilterChanged() {
        muatRekomendasi();
    }

    @FXML
    public void handleResetFilter() {
        if (comboWaktuLuang != null) comboWaktuLuang.setValue("Semua Durasi");
        if (comboKesibukan != null) comboKesibukan.setValue("Semua Kesibukan");
        if (comboUsia != null) comboUsia.setValue("Semua Usia");
        tipeDipilih = "semua";
        updateTipeButtonStyles();
        muatRekomendasi();
    }

    public void muatRekomendasi() {
        try {
            int maxDurasi = parseWaktuLuang(comboWaktuLuang != null ? comboWaktuLuang.getValue() : null);
            if (labelWaktuLuangInfo != null) {
                if (maxDurasi > 0) {
                    labelWaktuLuangInfo.setText("Aktivitas dijamin ≤ " + maxDurasi + " menit");
                } else {
                    labelWaktuLuangInfo.setText("Semua durasi aktivitas ditampilkan");
                }
            }

            String kesibukan = parseKesibukan(comboKesibukan != null ? comboKesibukan.getValue() : null);
            String usia = parseUsia(comboUsia != null ? comboUsia.getValue() : null);

            List<Rekomendasi> list = service.getRekomendasiMenuDanWorkout(tipeDipilih, kesibukan, usia);

            // Saring durasi jika tipe aktivitas atau workout dan ada batasan waktu luang (F-02)
            if (maxDurasi > 0 && list != null) {
                list = list.stream()
                    .filter(r -> "menu".equalsIgnoreCase(r.getTipe()) || r.getDurasiMenit() <= maxDurasi)
                    .toList();
            }

            if (labelJumlahRekomendasi != null && list != null) {
                labelJumlahRekomendasi.setText(list.size() + " Rekomendasi Tersedia");
            }

            renderCards(list);
        } catch (Exception e) {
            System.err.println("Info muat rekomendasi: " + e.getMessage());
        }
    }

    private void renderCards(List<Rekomendasi> list) {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            if (emptyStateBox != null) {
                emptyStateBox.setVisible(true);
                emptyStateBox.setManaged(true);
            }
            return;
        } else {
            if (emptyStateBox != null) {
                emptyStateBox.setVisible(false);
                emptyStateBox.setManaged(false);
            }
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
        if (btnTipeSemua != null) btnTipeSemua.getStyleClass().setAll("btn-outline");
        if (btnTipeAktivitas != null) btnTipeAktivitas.getStyleClass().setAll("btn-outline");
        if (btnTipeMenu != null) btnTipeMenu.getStyleClass().setAll("btn-outline");
        if (btnTipeWorkout != null) btnTipeWorkout.getStyleClass().setAll("btn-outline");

        if ("semua".equals(tipeDipilih) && btnTipeSemua != null) btnTipeSemua.getStyleClass().setAll("btn-primary");
        else if ("aktivitas".equals(tipeDipilih) && btnTipeAktivitas != null) btnTipeAktivitas.getStyleClass().setAll("btn-primary");
        else if ("menu".equals(tipeDipilih) && btnTipeMenu != null) btnTipeMenu.getStyleClass().setAll("btn-primary");
        else if ("workout".equals(tipeDipilih) && btnTipeWorkout != null) btnTipeWorkout.getStyleClass().setAll("btn-primary");
    }

    @FXML public void handleFilterTipeSemua() { tipeDipilih = "semua"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeAktivitas() { tipeDipilih = "aktivitas"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeMenu() { tipeDipilih = "menu"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeWorkout() { tipeDipilih = "workout"; updateTipeButtonStyles(); muatRekomendasi(); }
}
