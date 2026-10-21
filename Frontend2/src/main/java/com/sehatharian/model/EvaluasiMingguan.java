package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EvaluasiMingguan {
    private int id;
    private int penggunaId;
    private LocalDate mingguMulai;
    private double skorKonsistensi;
    private double durasiTidurRataRata;
    private boolean memenuhiStandarTidur;
    private String ringkasan;
    private String rekomendasi;
    private LocalDateTime dibuatPada;

    public EvaluasiMingguan() {}

    public EvaluasiMingguan(int id, int penggunaId, LocalDate mingguMulai, double skorKonsistensi, 
                            double durasiTidurRataRata, boolean memenuhiStandarTidur, 
                            String ringkasan, String rekomendasi) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.mingguMulai = mingguMulai;
        this.skorKonsistensi = skorKonsistensi;
        this.durasiTidurRataRata = durasiTidurRataRata;
        this.memenuhiStandarTidur = memenuhiStandarTidur;
        this.ringkasan = ringkasan;
        this.rekomendasi = rekomendasi;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getMingguMulai() { return mingguMulai; }
    public void setMingguMulai(LocalDate mingguMulai) { this.mingguMulai = mingguMulai; }

    public double getSkorKonsistensi() { return skorKonsistensi; }
    public void setSkorKonsistensi(double skorKonsistensi) { this.skorKonsistensi = skorKonsistensi; }

    public double getDurasiTidurRataRata() { return durasiTidurRataRata; }
    public void setDurasiTidurRataRata(double durasiTidurRataRata) { this.durasiTidurRataRata = durasiTidurRataRata; }

    public boolean isMemenuhiStandarTidur() { return memenuhiStandarTidur; }
    public void setMemenuhiStandarTidur(boolean memenuhiStandarTidur) { this.memenuhiStandarTidur = memenuhiStandarTidur; }

    public String getRingkasan() { return ringkasan; }
    public void setRingkasan(String ringkasan) { this.ringkasan = ringkasan; }

    public String getRekomendasi() { return rekomendasi; }
    public void setRekomendasi(String rekomendasi) { this.rekomendasi = rekomendasi; }

    public LocalDateTime getDibuatPada() { return dibuatPada; }
    public void setDibuatPada(LocalDateTime dibuatPada) { this.dibuatPada = dibuatPada; }

    public String getGradeKonsistensi() {
        if (skorKonsistensi >= 85) return "Sangat Konsisten (Luar Biasa!)";
        if (skorKonsistensi >= 70) return "Cukup Konsisten (Terus Tingkatkan)";
        if (skorKonsistensi >= 50) return "Mulai Bertumbuh (Perlu Disiplin Rutin)";
        return "Perlu Ditingkatkan (Mulai dari Langkah Kecil)";
    }
}
