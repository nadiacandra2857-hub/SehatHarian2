package com.sehatharian.model;

public class Tantangan {
    private int id;
    private String judul;
    private String deskripsi;
    private String kategori;          // jalan kaki, workout di rumah, peregangan, hidrasi, nutrisi
    private int durasiMenit;
    private String golonganKesibukan; // luar_rumah, dalam_rumah, semua
    private String golonganUsia;      // dewasa, lansia, semua

    public Tantangan() {}

    public Tantangan(int id, String judul, String deskripsi, String kategori, 
                     int durasiMenit, String golonganKesibukan, String golonganUsia) {
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
}
