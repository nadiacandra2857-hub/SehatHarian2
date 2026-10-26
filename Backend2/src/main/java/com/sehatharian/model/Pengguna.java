package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Pengguna {
    private int id;
    private String nama;
    private String email;
    private String passwordHash;
    private LocalDate tanggalLahir;
    private String golonganUsia;      // 'dewasa', 'lansia'
    private String golonganKesibukan; // 'luar_rumah', 'dalam_rumah'
    private int waktuLuangMenit;      // Waktu luang harian (menit)
    private boolean onboardingSelesai;
    private LocalDateTime createdAt;

    public Pengguna() {
        this.golonganUsia = "dewasa";
        this.golonganKesibukan = "luar_rumah";
        this.waktuLuangMenit = 30;
        this.onboardingSelesai = false;
    }

    public Pengguna(int id, String nama, LocalDate tanggalLahir, String golonganUsia, String golonganKesibukan, int waktuLuangMenit) {
        this.id = id;
        this.nama = nama;
        this.tanggalLahir = tanggalLahir;
        this.golonganUsia = golonganUsia;
        this.golonganKesibukan = golonganKesibukan;
        this.waktuLuangMenit = waktuLuangMenit;
        this.onboardingSelesai = true;
    }

    public Pengguna(int id, String nama, String email, String passwordHash, LocalDate tanggalLahir,
                    String golonganUsia, String golonganKesibukan, int waktuLuangMenit, boolean onboardingSelesai) {
        this.id = id;
        this.nama = nama;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tanggalLahir = tanggalLahir;
        this.golonganUsia = golonganUsia;
        this.golonganKesibukan = golonganKesibukan;
        this.waktuLuangMenit = waktuLuangMenit;
        this.onboardingSelesai = onboardingSelesai;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDate getTanggalLahir() { return tanggalLahir; }
    public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }

    public String getGolonganUsia() { return golonganUsia; }
    public void setGolonganUsia(String golonganUsia) { this.golonganUsia = golonganUsia; }

    public String getGolonganKesibukan() { return golonganKesibukan; }
    public void setGolonganKesibukan(String golonganKesibukan) { this.golonganKesibukan = golonganKesibukan; }

    public int getWaktuLuangMenit() { return waktuLuangMenit; }
    public void setWaktuLuangMenit(int waktuLuangMenit) { this.waktuLuangMenit = waktuLuangMenit; }

    public boolean isOnboardingSelesai() { return onboardingSelesai; }
    public void setOnboardingSelesai(boolean onboardingSelesai) { this.onboardingSelesai = onboardingSelesai; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLabelGolonganUsia() {
        if ("lansia".equalsIgnoreCase(golonganUsia)) {
            return "Lansia";
        }
        return "Dewasa";
    }

    public String getLabelGolonganKesibukan() {
        if ("dalam_rumah".equalsIgnoreCase(golonganKesibukan)) {
            return "Di Dalam Rumah";
        }
        return "Di Luar Rumah";
    }
}
