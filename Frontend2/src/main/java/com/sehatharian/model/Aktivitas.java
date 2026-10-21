package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Aktivitas {
    private int id;
    private int penggunaId;
    private LocalDate tanggal;
    private String jenisAktivitas;
    private int durasiMenit;
    private String keterangan;
    private LocalDateTime createdAt;

    public Aktivitas() {}

    public Aktivitas(int id, int penggunaId, LocalDate tanggal, String jenisAktivitas, 
                     int durasiMenit, String keterangan) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.tanggal = tanggal;
        this.jenisAktivitas = jenisAktivitas;
        this.durasiMenit = durasiMenit;
        this.keterangan = keterangan;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public String getJenisAktivitas() { return jenisAktivitas; }
    public void setJenisAktivitas(String jenisAktivitas) { this.jenisAktivitas = jenisAktivitas; }

    public int getDurasiMenit() { return durasiMenit; }
    public void setDurasiMenit(int durasiMenit) { this.durasiMenit = durasiMenit; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
