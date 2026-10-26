package com.sehatharian.model;

import java.time.LocalDateTime;

public class Tantangan {
    private int id;
    private String judul;
    private String deskripsi;
    private String kategori;
    private int durasiMenit;
    private String golonganKesibukan; // 'luar_rumah', 'dalam_rumah', 'semua'
    private String golonganUsia;      // 'dewasa', 'lansia', 'semua'
    private LocalDateTime createdAt;

    public Tantangan() {}

    public Tantangan(int id, String judul, String deskripsi, String kategori, int durasiMenit, String golonganKesibukan, String golonganUsia) {
        this.id = id;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.kategori = kategori;
        this.durasiMenit = durasiMenit;
        this.golonganKesibukan = golonganKesibukan;
        this.golonganUsia = golonganUsia;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public int getDurasiMenit() { return durasiMenit; }
    public void setDurasiMenit(int durasiMenit) { this.durasiMenit = durasiMenit; }

    public String getGolonganKesibukan() { return golonganKesibukan; }
    public void setGolonganKesibukan(String golonganKesibukan) { this.golonganKesibukan = golonganKesibukan; }

    public String getGolonganUsia() { return golonganUsia; }
    public void setGolonganUsia(String golonganUsia) { this.golonganUsia = golonganUsia; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
