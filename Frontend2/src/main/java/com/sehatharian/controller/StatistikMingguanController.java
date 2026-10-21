package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RingkasanKonsistensi;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.EvaluasiService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

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
        datePickerAkhir.setValue(tanggalAkhir);

        muatDataPengguna();
        muatDanTampilkanGrafik();
        AnimationHelper.fadeIn(statistikRoot, 350);
    }

    private void muatDataPengguna() {
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
            LocalDate tglMulai = tanggalAkhir.minusDays(6);
            labelPeriodeSubtitle.setText(String.format("Periode: %s s.d. %s (7 Hari Terakhir)",
                DateHelper.formatTanggalPendek(tglMulai), DateHelper.formatTanggalPendek(tanggalAkhir)));

            List<RingkasanKonsistensi> data7Hari = evaluasiService.getKonsistensi7HariTerakhir(penggunaAktif.getId(), tanggalAkhir);

            // Bersihkan chart
            chartKonsistensi.getData().clear();
            chartTidur.getData().clear();
            chartHidrasi.getData().clear();
            chartAktivitas.getData().clear();

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

            chartKonsistensi.getData().add(seriesKonsistensi);
            chartTidur.getData().add(seriesTidur);
            chartTidur.getData().add(seriesStandarTidur);
            chartHidrasi.getData().add(seriesHidrasi);
            chartAktivitas.getData().add(seriesAktivitas);

            // Perbarui Metric Cards
            double rataRataSkor = Math.round((totalSkor / 7.0) * 10.0) / 10.0;
            metricSkorLabel.setText(String.format("%.0f%%", rataRataSkor));

            double rataRataTidur = countTidur > 0 ? Math.round((totalJamTidur / countTidur) * 10.0) / 10.0 : 0.0;
            metricTidurLabel.setText(String.format("%.1f Jam", rataRataTidur));
            if (rataRataTidur >= 5.0) {
                metricTidurStandarLabel.setText("Status: Memenuhi standar PRD (≥ 5.0 jam)");
                metricTidurStandarLabel.setStyle("-fx-text-fill: #1E7E34; -fx-font-weight: 600;");
            } else {
                metricTidurStandarLabel.setText("Status: Di bawah standar PRD (< 5.0 jam)");
                metricTidurStandarLabel.setStyle("-fx-text-fill: #A03415; -fx-font-weight: 600;");
            }

            metricHidrasiLabel.setText(String.format("%,d ml", totalHidrasi));
            metricAktivitasLabel.setText(totalAktivitas + " Menit");

        } catch (SQLException e) {
            DialogHelper.showError("Database Error", "Gagal memuat grafik konsistensi: " + e.getMessage());
        }
    }

    @FXML
    public void handleUbahPeriode() {
        if (datePickerAkhir.getValue() != null) {
            tanggalAkhir = datePickerAkhir.getValue();
            muatDanTampilkanGrafik();
        }
    }

    @FXML
    public void handleSegarkan() {
        muatDanTampilkanGrafik();
    }
}
