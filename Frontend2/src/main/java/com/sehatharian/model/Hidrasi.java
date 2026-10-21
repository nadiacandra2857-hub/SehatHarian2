package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Hidrasi {
    private int id;
    private int penggunaId;
    private LocalDate tanggal;
    private int jumlahMl;
    private String catatan;
    private LocalDateTime createdAt;

    public Hidrasi() {}

    public Hidrasi(int id, int penggunaId, LocalDate tanggal, int jumlahMl, String catatan) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.tanggal = tanggal;
        this.jumlahMl = jumlahMl;
        this.catatan = catatan;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public int getJumlahMl() { return jumlahMl; }
    public void setJumlahMl(int jumlahMl) { this.jumlahMl = jumlahMl; }

    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getJumlahLiter() {
        return jumlahMl / 1000.0;
    }
}
