package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RingkasanKonsistensi;
import com.sehatharian.model.UserSession;
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

public class StatistikMingguanController implements AutoCloseable {
    private final UserSession session;
    private final com.sehatharian.model.SessionAccess access;

    public StatistikMingguanController(UserSession session) {
        this.session = java.util.Objects.requireNonNull(session);
        this.access = new com.sehatharian.model.SessionAccess(session);
        this.evaluasiService = new EvaluasiService(access);
    }


    @FXML private VBox statistikRoot;
    @FXML private Label labelPeriodeSubtitle;
    @FXML private Label labelCoverage;
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

    private final EvaluasiService evaluasiService;
    private Pengguna penggunaAktif;
    private LocalDate tanggalAkhir;

    @FXML
    public void initialize() {
        access.owner();
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
        penggunaAktif = session.requireActiveUser();
    }

    public void muatDanTampilkanGrafik() {
        access.owner();
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

            if(labelCoverage!=null) {
                long days=data7Hari.stream().filter(RingkasanKonsistensi::hasRecordedData).count();
                labelCoverage.setText(days + "/7 hari memiliki catatan. Hari tanpa catatan tidak berarti nilai nol. Rata-rata indikator memakai periode 7 hari; tidur adalah rencana, bukan pengukuran aktual.");
            }
            // Bersihkan chart
            if (chartKonsistensi != null) chartKonsistensi.getData().clear();
            if (chartTidur != null) chartTidur.getData().clear();
            if (chartHidrasi != null) chartHidrasi.getData().clear();
            if (chartAktivitas != null) chartAktivitas.getData().clear();

            XYChart.Series<String, Number> seriesKonsistensi = new XYChart.Series<>();
            seriesKonsistensi.setName("Skor Konsistensi");

            XYChart.Series<String, Number> seriesTidur = new XYChart.Series<>();
            seriesTidur.setName("Rencana tidur (jam)");

            XYChart.Series<String, Number> seriesStandarTidur = new XYChart.Series<>();
            seriesStandarTidur.setName("Aturan aplikasi (5 jam)");

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
                    if (rk.hasRecordedData()) seriesKonsistensi.getData().add(new XYChart.Data<>(labelHari, rk.getSkorHarianPersen()));
                    totalSkor += rk.getSkorHarianPersen();

                    // 2. Tidur
                    if (rk.isTidurTercatat()) seriesTidur.getData().add(new XYChart.Data<>(labelHari, rk.getDurasiTidurJam()));
                    seriesStandarTidur.getData().add(new XYChart.Data<>(labelHari, 5.0)); // Standar PRD 5 Jam
                    if (rk.getDurasiTidurJam() > 0) {
                        totalJamTidur += rk.getDurasiTidurJam();
                        countTidur++;
                    }

                    // 3. Hidrasi
                    if (rk.isHidrasiTercatat()) seriesHidrasi.getData().add(new XYChart.Data<>(labelHari, rk.getTotalHidrasiMl()));
                    totalHidrasi += rk.getTotalHidrasiMl();

                    // 4. Aktivitas
                    if (rk.isAktivitasTercatat()) seriesAktivitas.getData().add(new XYChart.Data<>(labelHari, rk.getDurasiAktivitasMenit()));
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
                metricSkorLabel.setText(data7Hari.stream().noneMatch(RingkasanKonsistensi::hasRecordedData) ? "Belum tercatat" : String.format(Locale.US, "%.0f%%", rataRataSkor));
            }

            double rataRataTidur = countTidur > 0 ? Math.round((totalJamTidur / countTidur) * 10.0) / 10.0 : 0.0;
            if (metricTidurLabel != null) {
                metricTidurLabel.setText(countTidur == 0 ? "Belum tercatat" : String.format(Locale.US, "%.1f Jam", rataRataTidur));
            }
            if (metricTidurStandarLabel != null) {
                if (countTidur == 0) {
                    metricTidurStandarLabel.setText("Belum ada catatan tidur; belum dinilai.");
                    metricTidurStandarLabel.setStyle("");
                } else if (rataRataTidur >= 5.0) {
                    metricTidurStandarLabel.setText("Status: Memenuhi standar PRD (≥ 5.0 jam)");
                    metricTidurStandarLabel.setStyle("-fx-text-fill: #1E7E34; -fx-font-weight: 600;");
                } else {
                    metricTidurStandarLabel.setText("Status: Di bawah standar PRD (< 5.0 jam)");
                    metricTidurStandarLabel.setStyle("-fx-text-fill: #A03415; -fx-font-weight: 600;");
                }
            }

            if (metricHidrasiLabel != null) {
                metricHidrasiLabel.setText(data7Hari.stream().noneMatch(RingkasanKonsistensi::isHidrasiTercatat) ? "Belum tercatat" : String.format(Locale.US, "%,d ml", totalHidrasi));
            }
            if (metricAktivitasLabel != null) {
                metricAktivitasLabel.setText(data7Hari.stream().noneMatch(RingkasanKonsistensi::isAktivitasTercatat) ? "Belum tercatat" : totalAktivitas + " Menit");
            }

        } catch (Exception e) {
            access.owner();
            if (chartKonsistensi != null) chartKonsistensi.getData().clear();
            if (chartTidur != null) chartTidur.getData().clear();
            if (chartHidrasi != null) chartHidrasi.getData().clear();
            if (chartAktivitas != null) chartAktivitas.getData().clear();
            if(labelCoverage!=null)labelCoverage.setText("Gagal memuat catatan. Segarkan untuk mencoba kembali; kelengkapan belum dapat dinilai.");
            if(metricHidrasiLabel!=null)metricHidrasiLabel.setText("Gagal memuat");
            if(metricAktivitasLabel!=null)metricAktivitasLabel.setText("Gagal memuat");
            if (metricSkorLabel != null) metricSkorLabel.setText("Gagal memuat");
            if (metricTidurLabel != null) metricTidurLabel.setText("Gagal memuat");
            if (metricTidurStandarLabel != null) metricTidurStandarLabel.setText("Belum dapat dinilai.");
        }
    }

    @FXML
    public void handleUbahPeriode() {
        access.owner();
        if (datePickerAkhir != null && datePickerAkhir.getValue() != null) {
            tanggalAkhir = datePickerAkhir.getValue();
            muatDanTampilkanGrafik();
        }
    }

    @FXML
    public void handleSegarkan() {
        access.owner();
        if (datePickerAkhir != null && datePickerAkhir.getValue() != null) {
            tanggalAkhir = datePickerAkhir.getValue();
        }
        muatDanTampilkanGrafik();
    }
    @Override public void close() { access.close(); }
}
