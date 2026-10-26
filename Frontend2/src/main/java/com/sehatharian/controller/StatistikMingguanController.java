package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RingkasanKonsistensi;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.EvaluasiService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class StatistikMingguanController {

    @FXML private VBox statistikRoot;
    @FXML private Label labelPeriodeSubtitle;
    @FXML private DatePicker datePickerAkhir;

    @FXML private Label metricSkorLabel;
    @FXML private Label metricTidurLabel;
    @FXML private Label metricTidurStandarLabel;
    @FXML private Label metricHidrasiLabel;
    @FXML private Label metricAktivitasLabel;

    @FXML private BarChart<String, Number> chartKonsistensi;
    @FXML private LineChart<String, Number> chartTidur;
    @FXML private BarChart<String, Number> chartHidrasi;
    @FXML private BarChart<String, Number> chartAktivitas;

    private final EvaluasiService evaluasiService = new EvaluasiService();
    private final PenggunaRepository penggunaRepo = new PenggunaRepository();
    private Pengguna penggunaAktif;
    private LocalDate tanggalAkhir;

    @FXML
    public void initialize() {
        tanggalAkhir = LocalDate.now();
        if (datePickerAkhir != null) {
            datePickerAkhir.setValue(tanggalAkhir);
        }

        // Matikan animasi grafik untuk mencegah glitch konkurensi saat pembaruan data
        if (chartKonsistensi != null) chartKonsistensi.setAnimated(false);
        if (chartTidur != null) chartTidur.setAnimated(false);
        if (chartHidrasi != null) chartHidrasi.setAnimated(false);
        if (chartAktivitas != null) chartAktivitas.setAnimated(false);

        muatDataPengguna();
        muatDanTampilkanGrafik();
        if (statistikRoot != null) {
            AnimationHelper.fadeIn(statistikRoot, 350);
        }
    }

    private void muatDataPengguna() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.getActiveUser() != null) {
            penggunaAktif = session.getActiveUser();
            return;
        } else if (session.isLoggedIn() && session.getUserId() > 0) {
            try {
                penggunaAktif = penggunaRepo.getById(session.getUserId());
                if (penggunaAktif != null) return;
            } catch (Exception ignored) {}
        }

        try {
            penggunaAktif = penggunaRepo.getDefaultPengguna();
        } catch (SQLException ignored) {}
        if (penggunaAktif == null) {
            penggunaAktif = new Pengguna(1, "Nadia", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 30);
        }
    }

    public void muatDanTampilkanGrafik() {
        if (penggunaAktif == null) return;
        try {
            if (tanggalAkhir == null) {
                tanggalAkhir = LocalDate.now();
            }
            LocalDate tglMulai = tanggalAkhir.minusDays(6);
            if (labelPeriodeSubtitle != null) {
                labelPeriodeSubtitle.setText(String.format("Periode: %s s.d. %s (7 Hari Terakhir)",
                    DateHelper.formatTanggalPendek(tglMulai), DateHelper.formatTanggalPendek(tanggalAkhir)));
            }

            List<RingkasanKonsistensi> data7Hari = evaluasiService.getKonsistensi7HariTerakhir(penggunaAktif.getId(), tanggalAkhir);

            // Bersihkan chart
            if (chartKonsistensi != null) chartKonsistensi.getData().clear();
            if (chartTidur != null) chartTidur.getData().clear();
            if (chartHidrasi != null) chartHidrasi.getData().clear();
            if (chartAktivitas != null) chartAktivitas.getData().clear();

            XYChart.Series<String, Number> seriesKonsistensi = new XYChart.Series<>();
            seriesKonsistensi.setName("Skor Konsistensi");

            XYChart.Series<String, Number> seriesTidur = new XYChart.Series<>();
            seriesTidur.setName("Durasi Tidur (Jam)");

            XYChart.Series<String, Number> seriesStandarTidur = new XYChart.Series<>();
            seriesStandarTidur.setName("Standar PRD (5 Jam)");

            XYChart.Series<String, Number> seriesHidrasi = new XYChart.Series<>();
            seriesHidrasi.setName("Asupan Air (ml)");

            XYChart.Series<String, Number> seriesAktivitas = new XYChart.Series<>();
            seriesAktivitas.setName("Aktivitas (Menit)");

            double totalSkor = 0;
            double totalJamTidur = 0;
            int countTidur = 0;
            int totalHidrasi = 0;
            int totalAktivitas = 0;

            if (data7Hari != null) {
                for (RingkasanKonsistensi rk : data7Hari) {
                    String labelHari = rk.getLabelHari() + " (" + rk.getTanggal().getDayOfMonth() + ")";

                    // 1. Konsistensi
                    seriesKonsistensi.getData().add(new XYChart.Data<>(labelHari, rk.getSkorHarianPersen()));
                    totalSkor += rk.getSkorHarianPersen();

                    // 2. Tidur
                    seriesTidur.getData().add(new XYChart.Data<>(labelHari, rk.getDurasiTidurJam()));
                    seriesStandarTidur.getData().add(new XYChart.Data<>(labelHari, 5.0)); // Standar PRD 5 Jam
                    if (rk.getDurasiTidurJam() > 0) {
                        totalJamTidur += rk.getDurasiTidurJam();
                        countTidur++;
                    }

                    // 3. Hidrasi
                    seriesHidrasi.getData().add(new XYChart.Data<>(labelHari, rk.getTotalHidrasiMl()));
                    totalHidrasi += rk.getTotalHidrasiMl();

                    // 4. Aktivitas
                    seriesAktivitas.getData().add(new XYChart.Data<>(labelHari, rk.getDurasiAktivitasMenit()));
                    totalAktivitas += rk.getDurasiAktivitasMenit();
                }
            }

            if (chartKonsistensi != null) chartKonsistensi.getData().add(seriesKonsistensi);
            if (chartTidur != null) {
                chartTidur.getData().add(seriesTidur);
                chartTidur.getData().add(seriesStandarTidur);
            }
            if (chartHidrasi != null) chartHidrasi.getData().add(seriesHidrasi);
            if (chartAktivitas != null) chartAktivitas.getData().add(seriesAktivitas);

            // Perbarui Metric Cards
            int countHari = (data7Hari != null && !data7Hari.isEmpty()) ? data7Hari.size() : 7;
            double rataRataSkor = countHari > 0 ? Math.round((totalSkor / countHari) * 10.0) / 10.0 : 0.0;
            if (metricSkorLabel != null) {
                metricSkorLabel.setText(String.format(Locale.US, "%.0f%%", rataRataSkor));
            }

            double rataRataTidur = countTidur > 0 ? Math.round((totalJamTidur / countTidur) * 10.0) / 10.0 : 0.0;
            if (metricTidurLabel != null) {
                metricTidurLabel.setText(String.format(Locale.US, "%.1f Jam", rataRataTidur));
            }
            if (metricTidurStandarLabel != null) {
                if (rataRataTidur >= 5.0) {
                    metricTidurStandarLabel.setText("Status: Memenuhi standar PRD (≥ 5.0 jam)");
                    metricTidurStandarLabel.setStyle("-fx-text-fill: #1E7E34; -fx-font-weight: 600;");
                } else {
                    metricTidurStandarLabel.setText("Status: Di bawah standar PRD (< 5.0 jam)");
                    metricTidurStandarLabel.setStyle("-fx-text-fill: #A03415; -fx-font-weight: 600;");
                }
            }

            if (metricHidrasiLabel != null) {
                metricHidrasiLabel.setText(String.format(Locale.US, "%,d ml", totalHidrasi));
            }
            if (metricAktivitasLabel != null) {
                metricAktivitasLabel.setText(totalAktivitas + " Menit");
            }

        } catch (Exception e) {
            System.err.println("Info muat statistik: " + e.getMessage());
        }
    }

    @FXML
    public void handleUbahPeriode() {
        if (datePickerAkhir != null && datePickerAkhir.getValue() != null) {
            tanggalAkhir = datePickerAkhir.getValue();
            muatDanTampilkanGrafik();
        }
    }

    @FXML
    public void handleSegarkan() {
        if (datePickerAkhir != null && datePickerAkhir.getValue() != null) {
            tanggalAkhir = datePickerAkhir.getValue();
        }
        muatDanTampilkanGrafik();
    }
}
