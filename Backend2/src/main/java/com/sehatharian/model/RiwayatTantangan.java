package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RiwayatTantangan {
    private int id;
    private int penggunaId;
    private int tantanganId;
    private LocalDate tanggal;
    private int durasiTargetMenit;
    private String status;            // 'disarankan', 'selesai', 'dilewati'
    private LocalDateTime diselesaikanPada;
    private LocalDateTime createdAt;

    // Relasi/Join data dari tantangan
    private String tantanganJudul;
    private String tantanganDeskripsi;
    private String tantanganKategori;

    public RiwayatTantangan() {}

    public RiwayatTantangan(int id, int penggunaId, int tantanganId, LocalDate tanggal,
                            int durasiTargetMenit, String status, LocalDateTime diselesaikanPada) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.tantanganId = tantanganId;
        this.tanggal = tanggal;
        this.durasiTargetMenit = durasiTargetMenit;
        this.status = status;
        this.diselesaikanPada = diselesaikanPada;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public int getTantanganId() { return tantanganId; }
    public void setTantanganId(int tantanganId) { this.tantanganId = tantanganId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public int getDurasiTargetMenit() { return durasiTargetMenit; }
    public void setDurasiTargetMenit(int durasiTargetMenit) { this.durasiTargetMenit = durasiTargetMenit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDiselesaikanPada() { return diselesaikanPada; }
    public void setDiselesaikanPada(LocalDateTime diselesaikanPada) { this.diselesaikanPada = diselesaikanPada; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTantanganJudul() { return tantanganJudul; }
    public void setTantanganJudul(String tantanganJudul) { this.tantanganJudul = tantanganJudul; }

    public String getTantanganDeskripsi() { return tantanganDeskripsi; }
    public void setTantanganDeskripsi(String tantanganDeskripsi) { this.tantanganDeskripsi = tantanganDeskripsi; }

    public String getTantanganKategori() { return tantanganKategori; }
    public void setTantanganKategori(String tantanganKategori) { this.tantanganKategori = tantanganKategori; }

    public boolean isSelesai() {
        return "selesai".equalsIgnoreCase(status);
    }

    public boolean isDilewati() {
        return "dilewati".equalsIgnoreCase(status);
    }

    public boolean isDisarankan() {
        return "disarankan".equalsIgnoreCase(status);
    }
}
