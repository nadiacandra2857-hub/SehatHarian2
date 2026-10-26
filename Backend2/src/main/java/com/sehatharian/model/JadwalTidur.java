package com.sehatharian.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class JadwalTidur {
    private int id;
    private int penggunaId;
    private LocalDate tanggal;
    private LocalTime jamTidur;
    private LocalTime jamBangun;
    private double durasiJam;
    private boolean memenuhiStandar; // TRUE jika >= 5.0 jam
    private boolean pengingatAktif;
    private LocalDateTime createdAt;

    public JadwalTidur() {}

    public JadwalTidur(int id, int penggunaId, LocalDate tanggal, LocalTime jamTidur, LocalTime jamBangun,
                       double durasiJam, boolean memenuhiStandar, boolean pengingatAktif) {
        this.id = id;
        this.penggunaId = penggunaId;
        this.tanggal = tanggal;
        this.jamTidur = jamTidur;
        this.jamBangun = jamBangun;
        this.durasiJam = durasiJam;
        this.memenuhiStandar = memenuhiStandar;
        this.pengingatAktif = pengingatAktif;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPenggunaId() { return penggunaId; }
    public void setPenggunaId(int penggunaId) { this.penggunaId = penggunaId; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public LocalTime getJamTidur() { return jamTidur; }
    public void setJamTidur(LocalTime jamTidur) { this.jamTidur = jamTidur; }

    public LocalTime getJamBangun() { return jamBangun; }
    public void setJamBangun(LocalTime jamBangun) { this.jamBangun = jamBangun; }

    public double getDurasiJam() { return durasiJam; }
    public void setDurasiJam(double durasiJam) { this.durasiJam = durasiJam; }

    public boolean isMemenuhiStandar() { return memenuhiStandar; }
    public void setMemenuhiStandar(boolean memenuhiStandar) { this.memenuhiStandar = memenuhiStandar; }

    public boolean isPengingatAktif() { return pengingatAktif; }
    public void setPengingatAktif(boolean pengingatAktif) { this.pengingatAktif = pengingatAktif; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
