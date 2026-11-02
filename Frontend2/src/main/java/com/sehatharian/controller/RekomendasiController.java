package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.Rekomendasi;
import com.sehatharian.model.UserSession;
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

public class RekomendasiController implements AutoCloseable {
    private final UserSession session;
    private final com.sehatharian.model.SessionAccess access;

    public RekomendasiController(UserSession session) {
        this(session, new RekomendasiService());
    }

    public RekomendasiController(UserSession session, RekomendasiService service) {
        this.session = java.util.Objects.requireNonNull(session);
        this.access = new com.sehatharian.model.SessionAccess(session);
        this.service = java.util.Objects.requireNonNull(service);
    }


    @FXML private VBox rekomendasiRoot;
    @FXML private Label labelJumlahRekomendasi;
    @FXML private Label labelSumberData;
    private boolean demoPreview;

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

    private final RekomendasiService service;
    private Pengguna penggunaAktif;

    private String tipeDipilih = "semua";

    @FXML
    public void initialize() {
        access.owner();
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
        penggunaAktif = session.requireActiveUser();

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
        access.owner();
        muatRekomendasi();
    }

    @FXML
    public void handleResetFilter() {
        access.owner();
        if (comboWaktuLuang != null) comboWaktuLuang.setValue("Semua Durasi");
        if (comboKesibukan != null) comboKesibukan.setValue("Semua Kesibukan");
        if (comboUsia != null) comboUsia.setValue("Semua Usia");
        tipeDipilih = "semua";
        updateTipeButtonStyles();
        muatRekomendasi();
    }

    public void muatRekomendasi() {
        access.owner();
        try {
            var profile=session.requireActiveUser();
            if(!demoPreview && (profile.getWaktuLuangMenit()<=0 ||
                !java.util.Set.of("dewasa","lansia").contains(String.valueOf(profile.getGolonganUsia())) ||
                !java.util.Set.of("dalam_rumah","luar_rumah","semua").contains(String.valueOf(profile.getGolonganKesibukan())))) {
                if(cardsContainer!=null)cardsContainer.getChildren().clear();
                if(emptyStateBox!=null){emptyStateBox.setVisible(false);emptyStateBox.setManaged(false);}
                if(labelSumberData!=null)labelSumberData.setText("Belum cukup data. Lengkapi waktu luang, kelompok usia dan rutinitas di Profil untuk rekomendasi database. Pratinjau contoh tetap tersedia secara eksplisit.");
                if(labelJumlahRekomendasi!=null)labelJumlahRekomendasi.setText("Profil belum lengkap");
                return;
            }
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

            var result = demoPreview
                ? com.sehatharian.service.DemoCatalog.recommendations(tipeDipilih, kesibukan, usia)
                : service.bacaKatalog(tipeDipilih, kesibukan, usia);
            access.owner();
            if (labelSumberData != null) labelSumberData.setText(result.message());
            if (result.source() == com.sehatharian.storage.ReadResult.Source.UNAVAILABLE) {
                if (cardsContainer != null) cardsContainer.getChildren().clear();
                if (emptyStateBox != null) {
                    emptyStateBox.setVisible(false);
                    emptyStateBox.setManaged(false);
                }
                if (labelJumlahRekomendasi != null) labelJumlahRekomendasi.setText("Belum dapat dimuat");
                return;
            }
            List<Rekomendasi> list = result.value().orElseThrow();

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
            access.owner();
            if (cardsContainer != null) cardsContainer.getChildren().clear();
            if (emptyStateBox != null) {
                emptyStateBox.setVisible(false);
                emptyStateBox.setManaged(false);
            }
            if (labelJumlahRekomendasi != null) labelJumlahRekomendasi.setText("Belum dapat dimuat");
            if (labelSumberData != null) labelSumberData.setText("Katalog gagal dimuat. Silakan coba muat kembali.");
        }
    }

    @FXML public void handlePreviewDemo() {
        access.owner();
        demoPreview = true;
        muatRekomendasi();
    }

    @FXML public void handleLoadDatabase() {
        access.owner();
        demoPreview = false;
        muatRekomendasi();
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

            HBox topRow = new com.sehatharian.util.AdaptiveRow(10);
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

            Label badgeKesesuaian = new Label(demoPreview ? "Pratinjau contoh" : "Katalog database");
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
            HBox bottomRow = new com.sehatharian.util.AdaptiveRow(8);
            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.setStyle("-fx-padding: 6 0 0 0;");

            String guidanceText;
            guidanceText = "Alasan ditampilkan: cocok dengan filter kategori, rutinitas dan usia yang dipilih" +
                (parseWaktuLuang(comboWaktuLuang.getValue())>0 && !"menu".equalsIgnoreCase(r.getTipe()) ? "; durasi dalam batas waktu pilihan." : ".") +
                ("menu".equalsIgnoreCase(r.getTipe()) ? " Preferensi bahan, porsi dan pantangan belum dimodelkan." : " Foto adalah ilustrasi suasana, bukan panduan teknik.");

            Label lblGuidance = new Label(guidanceText);
            lblGuidance.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #5A6D63;");

            lblGuidance.setWrapText(true); lblJudul.setWrapText(true);
            bottomRow.getChildren().add(lblGuidance);

            VBox content=new VBox(10,topRow,lblJudul,lblDesc,bottomRow);content.setMinWidth(0);HBox.setHgrow(content,Priority.ALWAYS);
            var composition=new com.sehatharian.util.AdaptiveRow(16);
            composition.getChildren().addAll(new com.sehatharian.util.EditorialImage("menu".equalsIgnoreCase(r.getTipe())?1:3,180,120),content);
            card.getChildren().add(composition);
            cardsContainer.getChildren().add(card);
            card.setMinWidth(0);
            card.lookupAll(".label").forEach(n -> { if(n instanceof Label l){l.setWrapText(true);l.setMinWidth(0);} });
        }
    }

    private int parseWaktuLuang(String val) {
        if (val == null || val.startsWith("Semua")) return 0;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            access.owner();
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

    @FXML public void handleFilterTipeSemua() {
        access.owner(); tipeDipilih = "semua"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeAktivitas() {
        access.owner(); tipeDipilih = "aktivitas"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeMenu() {
        access.owner(); tipeDipilih = "menu"; updateTipeButtonStyles(); muatRekomendasi(); }
    @FXML public void handleFilterTipeWorkout() {
        access.owner(); tipeDipilih = "workout"; updateTipeButtonStyles(); muatRekomendasi(); }
    @Override public void close() { access.close(); }
}
