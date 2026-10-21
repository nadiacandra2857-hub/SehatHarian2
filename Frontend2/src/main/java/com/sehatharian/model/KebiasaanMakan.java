package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class KebiasaanMakan {
    private int id;
    private int penggunaId;
    private LocalDate tanggal;
    private String waktuMakan; // sarapan, makan_siang, makan_malam, camilan
    private String menu;
    private String keterangan;
    private LocalDateTime createdAt;

    public KebiasaanMakan() {}

    public KebiasaanMakan(int id, int penggunaId, LocalDate tanggal, String waktuMakan, 
                          String menu, String keterangan) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.tanggal = tanggal;
        this.waktuMakan = waktuMakan;
        this.menu = menu;
        this.keterangan = keterangan;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public String getWaktuMakan() { return waktuMakan; }
    public void setWaktuMakan(String waktuMakan) { this.waktuMakan = waktuMakan; }

    public String getMenu() { return menu; }
    public void setMenu(String menu) { this.menu = menu; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLabelWaktuMakan() {
        if ("sarapan".equalsIgnoreCase(waktuMakan)) return "Sarapan";
        if ("makan_siang".equalsIgnoreCase(waktuMakan)) return "Makan Siang";
        if ("makan_malam".equalsIgnoreCase(waktuMakan)) return "Makan Malam";
        if ("camilan".equalsIgnoreCase(waktuMakan)) return "Camilan Sehat";
        return waktuMakan;
    }
}
