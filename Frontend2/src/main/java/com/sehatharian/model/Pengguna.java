package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Pengguna {
    private int id;
    private String nama;
    private LocalDate tanggalLahir;
    private String golonganUsia;       // "dewasa" atau "lansia"
    private String golonganKesibukan;  // "luar_rumah" atau "dalam_rumah"
    private int waktuLuangMenit;
    private LocalDateTime createdAt;

    public Pengguna() {}

    public Pengguna(int id, String nama, LocalDate tanggalLahir, String golonganUsia, 
                    String golonganKesibukan, int waktuLuangMenit) {
        this.id = id;
        this.nama = nama;
        this.tanggalLahir = tanggalLahir;
        this.golonganUsia = golonganUsia;
        this.golonganKesibukan = golonganKesibukan;
        this.waktuLuangMenit = waktuLuangMenit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public LocalDate getTanggalLahir() { return tanggalLahir; }
    public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }

    public String getGolonganUsia() { return golonganUsia; }
    public void setGolonganUsia(String golonganUsia) { this.golonganUsia = golonganUsia; }

    public String getGolonganKesibukan() { return golonganKesibukan; }
    public void setGolonganKesibukan(String golonganKesibukan) { this.golonganKesibukan = golonganKesibukan; }

    public int getWaktuLuangMenit() { return waktuLuangMenit; }
    public void setWaktuLuangMenit(int waktuLuangMenit) { this.waktuLuangMenit = waktuLuangMenit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLabelGolonganUsia() {
        if ("lansia".equalsIgnoreCase(golonganUsia)) return "Lansia";
        return "Dewasa";
    }

    public String getLabelGolonganKesibukan() {
        if ("dalam_rumah".equalsIgnoreCase(golonganKesibukan)) return "Di Dalam Rumah (WFH/Domestik)";
        return "Di Luar Rumah (Pekerja Lapangan/Kantor)";
    }
}
