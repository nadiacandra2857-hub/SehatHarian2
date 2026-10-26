package com.sehatharian.model;

import java.time.LocalDateTime;

public class Rekomendasi {
    private int id;
    private String tipe;              // 'aktivitas', 'menu', 'workout'
    private String judul;
    private String deskripsi;
    private String golonganKesibukan; // 'luar_rumah', 'dalam_rumah', 'semua'
    private String golonganUsia;      // 'dewasa', 'lansia', 'semua'
    private int durasiMenit;
    private LocalDateTime createdAt;

    public Rekomendasi() {}

    public Rekomendasi(int id, String tipe, String judul, String deskripsi, String golonganKesibukan, String golonganUsia, int durasiMenit) {
        this.id = id;
        this.tipe = tipe;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.golonganKesibukan = golonganKesibukan;
        this.golonganUsia = golonganUsia;
        this.durasiMenit = durasiMenit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }

    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getGolonganKesibukan() { return golonganKesibukan; }
    public void setGolonganKesibukan(String golonganKesibukan) { this.golonganKesibukan = golonganKesibukan; }

    public String getGolonganUsia() { return golonganUsia; }
    public void setGolonganUsia(String golonganUsia) { this.golonganUsia = golonganUsia; }

    public int getDurasiMenit() { return durasiMenit; }
    public void setDurasiMenit(int durasiMenit) { this.durasiMenit = durasiMenit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLabelTipe() {
        if (tipe == null) return "-";
        switch (tipe.toLowerCase()) {
            case "aktivitas": return "Aktivitas";
            case "menu": return "Menu Sehat";
            case "workout": return "Workout";
            default: return tipe;
        }
    }
}
