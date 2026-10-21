package com.sehatharian.model;

import java.time.LocalDate;

public class RingkasanKonsistensi {
    private LocalDate tanggal;
    private String labelHari;          // Misal: "Sen", "Sel", "Rab"
    private int tantanganSelesai;
    private int totalTantangan;
    private int durasiAktivitasMenit;
    private int totalHidrasiMl;
    private double durasiTidurJam;
    private double skorHarianPersen;    // 0 - 100%

    public RingkasanKonsistensi(LocalDate tanggal, String labelHari) {
        this.tanggal = tanggal;
        this.labelHari = labelHari;
    }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public String getLabelHari() { return labelHari; }
    public void setLabelHari(String labelHari) { this.labelHari = labelHari; }

    public int getTantanganSelesai() { return tantanganSelesai; }
    public void setTantanganSelesai(int tantanganSelesai) { this.tantanganSelesai = tantanganSelesai; }

    public int getTotalTantangan() { return totalTantangan; }
    public void setTotalTantangan(int totalTantangan) { this.totalTantangan = totalTantangan; }

    public int getDurasiAktivitasMenit() { return durasiAktivitasMenit; }
    public void setDurasiAktivitasMenit(int durasiAktivitasMenit) { this.durasiAktivitasMenit = durasiAktivitasMenit; }

    public int getTotalHidrasiMl() { return totalHidrasiMl; }
    public void setTotalHidrasiMl(int totalHidrasiMl) { this.totalHidrasiMl = totalHidrasiMl; }

    public double getDurasiTidurJam() { return durasiTidurJam; }
    public void setDurasiTidurJam(double durasiTidurJam) { this.durasiTidurJam = durasiTidurJam; }

    public double getSkorHarianPersen() { return skorHarianPersen; }
    public void setSkorHarianPersen(double skorHarianPersen) { this.skorHarianPersen = skorHarianPersen; }
}
