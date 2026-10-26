package com.sehatharian.service;

import com.sehatharian.model.JadwalTidur;
import com.sehatharian.repository.JadwalTidurRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TidurService {
    public static final double STANDAR_MINIMAL_TIDUR_JAM = 5.0;
    private final JadwalTidurRepository repo;

    public TidurService() {
        this.repo = new JadwalTidurRepository();
    }

    public TidurService(JadwalTidurRepository repo) {
        this.repo = repo;
    }

    /**
     * Menghitung durasi tidur dalam jam (desimal) dengan penanganan lintas tengah malam.
     * Contoh:
     * - 22:00 ke 06:00 -> 8.0 jam
     * - 23:30 ke 06:00 -> 6.5 jam
     * - 01:00 ke 06:30 -> 5.5 jam
     */
    public double hitungDurasiTidur(LocalTime jamTidur, LocalTime jamBangun) {
        if (jamTidur == null || jamBangun == null) return 0.0;
        int menitTidur = jamTidur.getHour() * 60 + jamTidur.getMinute();
        int menitBangun = jamBangun.getHour() * 60 + jamBangun.getMinute();
        int diff = menitBangun - menitTidur;
        if (diff < 0) {
            diff += 24 * 60;
        }

        double jam = diff / 60.0;
        return Math.round(jam * 100.0) / 100.0;
    }

    /**
     * Memeriksa apakah durasi tidur memenuhi standar PRD (minimal 5 jam).
     * Jika durasi < 5.0 jam, otomatis memicu aturan tidur kurang / evaluasi adaptif.
     */
    public boolean apakahMemenuhiStandar(double durasiJam) {
        return durasiJam >= STANDAR_MINIMAL_TIDUR_JAM;
    }

    /**
     * F-07: Mencatat jam tidur manual ke database.
     */
    public JadwalTidur catatJamTidur(int penggunaId, LocalDate tanggal, LocalTime jamTidur, 
                                     LocalTime jamBangun, boolean pengingatAktif) throws SQLException, IllegalArgumentException {
        if (jamTidur == null || jamBangun == null) {
            throw new IllegalArgumentException("Waktu tidur dan waktu bangun wajib diisi.");
        }

        double durasi = hitungDurasiTidur(jamTidur, jamBangun);
        if (durasi < 0.5) {
            throw new IllegalArgumentException("Durasi tidur terlalu singkat (kurang dari 30 menit). Periksa kembali jam input.");
        }
        if (durasi > 20.0) {
            throw new IllegalArgumentException("Durasi tidur tidak wajar (lebih dari 20 jam). Periksa kembali jam input.");
        }

        boolean memenuhiStandar = apakahMemenuhiStandar(durasi);

        JadwalTidur j = new JadwalTidur(0, penggunaId, tanggal, jamTidur, jamBangun, durasi, memenuhiStandar, pengingatAktif);
        int generatedId = repo.simpan(j);
        j.setId(generatedId);
        return j;
    }

    public List<JadwalTidur> getRiwayatTidur(int penggunaId, int limit) throws SQLException {
        return repo.getRiwayatByPengguna(penggunaId, limit);
    }

    public JadwalTidur getJadwalTerbaru(int penggunaId) throws SQLException {
        return repo.getTerbaruByPengguna(penggunaId);
    }

    public boolean updatePengingat(int id, boolean aktif) throws SQLException {
        return repo.setPengingatAktif(id, aktif);
    }

    public boolean hapusCatatan(int id) throws SQLException {
        return repo.hapus(id);
    }
}
