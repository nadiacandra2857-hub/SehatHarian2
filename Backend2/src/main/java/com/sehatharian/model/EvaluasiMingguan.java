package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EvaluasiMingguan {
    private int id;
    private int penggunaId;
    private LocalDate mingguMulai;
    private double skorKonsistensi;
    private Double durasiTidurRataRata;
    private boolean memenuhiStandarTidur;
    private String ringkasan;
    private String rekomendasi;
    private LocalDateTime dibuatPada;

    public EvaluasiMingguan() {}

    public EvaluasiMingguan(int id, int penggunaId, LocalDate mingguMulai, double skorKonsistensi,
                            Double durasiTidurRataRata, boolean memenuhiStandarTidur, String ringkasan, String rekomendasi) {
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

    public Double getDurasiTidurRataRata() { return durasiTidurRataRata; }
    public void setDurasiTidurRataRata(Double durasiTidurRataRata) { this.durasiTidurRataRata = durasiTidurRataRata; }

    public boolean isMemenuhiStandarTidur() { return memenuhiStandarTidur; }
    public void setMemenuhiStandarTidur(boolean memenuhiStandarTidur) { this.memenuhiStandarTidur = memenuhiStandarTidur; }

    public String getRingkasan() { return ringkasan; }
    public void setRingkasan(String ringkasan) { this.ringkasan = ringkasan; }

    public String getRekomendasi() { return rekomendasi; }
    public void setRekomendasi(String rekomendasi) { this.rekomendasi = rekomendasi; }

    public LocalDateTime getDibuatPada() { return dibuatPada; }
    public void setDibuatPada(LocalDateTime dibuatPada) { this.dibuatPada = dibuatPada; }

    public String getGradeKonsistensi() {
        if (skorKonsistensi >= 85) return "Sangat Baik (A)";
        if (skorKonsistensi >= 70) return "Baik (B)";
        if (skorKonsistensi >= 55) return "Cukup (C)";
        return "Perlu Ditingkatkan (D)";
    }
}
