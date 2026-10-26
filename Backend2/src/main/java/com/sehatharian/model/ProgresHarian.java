package com.sehatharian.model;

import java.time.LocalDate;

/**
 * ProgresHarian merangkum metrik capaian kebiasaan sehat pengguna untuk satu hari tertentu.
 * Digunakan untuk integrasi dasbor, grafik, dan evaluasi.
 */
public class ProgresHarian {
    private int penggunaId;
    private LocalDate tanggal;

    // Metrik Tantangan
    private int tantanganSelesai;
    private int totalTantangan;
    private double persenTantangan;

    // Metrik Hidrasi
    private int totalHidrasiMl;
    private int targetHidrasiMl;
    private double persenHidrasi;

    // Metrik Aktivitas Fisik
    private int totalAktivitasMenit;
    private int targetAktivitasMenit;
    private double persenAktivitas;

    // Metrik Tidur
    private double durasiTidurJam;
    private boolean tidurMemenuhiStandar;

    // Skor Komposit Harian (0.0 - 100.0%)
    private double skorKompositHarian;

    public ProgresHarian() {
        this.targetHidrasiMl = 2000;
        this.targetAktivitasMenit = 30;
    }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public int getTantanganSelesai() { return tantanganSelesai; }
    public void setTantanganSelesai(int tantanganSelesai) { this.tantanganSelesai = tantanganSelesai; }

    public int getTotalTantangan() { return totalTantangan; }
    public void setTotalTantangan(int totalTantangan) { this.totalTantangan = totalTantangan; }

    public double getPersenTantangan() { return persenTantangan; }
    public void setPersenTantangan(double persenTantangan) { this.persenTantangan = persenTantangan; }

    public int getTotalHidrasiMl() { return totalHidrasiMl; }
    public void setTotalHidrasiMl(int totalHidrasiMl) { this.totalHidrasiMl = totalHidrasiMl; }

    public int getTargetHidrasiMl() { return targetHidrasiMl; }
    public void setTargetHidrasiMl(int targetHidrasiMl) { this.targetHidrasiMl = targetHidrasiMl; }

    public double getPersenHidrasi() { return persenHidrasi; }
    public void setPersenHidrasi(double persenHidrasi) { this.persenHidrasi = persenHidrasi; }

    public int getTotalAktivitasMenit() { return totalAktivitasMenit; }
    public void setTotalAktivitasMenit(int totalAktivitasMenit) { this.totalAktivitasMenit = totalAktivitasMenit; }

    public int getTargetAktivitasMenit() { return targetAktivitasMenit; }
    public void setTargetAktivitasMenit(int targetAktivitasMenit) { this.targetAktivitasMenit = targetAktivitasMenit; }

    public double getPersenAktivitas() { return persenAktivitas; }
    public void setPersenAktivitas(double persenAktivitas) { this.persenAktivitas = persenAktivitas; }

    public double getDurasiTidurJam() { return durasiTidurJam; }
    public void setDurasiTidurJam(double durasiTidurJam) { this.durasiTidurJam = durasiTidurJam; }

    public boolean isTidurMemenuhiStandar() { return tidurMemenuhiStandar; }
    public void setTidurMemenuhiStandar(boolean tidurMemenuhiStandar) { this.tidurMemenuhiStandar = tidurMemenuhiStandar; }

    public double getSkorKompositHarian() { return skorKompositHarian; }
    public void setSkorKompositHarian(double skorKompositHarian) { this.skorKompositHarian = skorKompositHarian; }
}
