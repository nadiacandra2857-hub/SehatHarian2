package com.sehatharian.service;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.JadwalTidur;
import com.sehatharian.repository.JadwalTidurRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TidurService {
    private final SessionAccess access;
    public static final double STANDAR_MINIMAL_TIDUR_JAM = 5.0;
    private final JadwalTidurRepository repo;

    public TidurService(SessionAccess access) {
        this.access = java.util.Objects.requireNonNull(access);
        this.repo = new JadwalTidurRepository(access);
    }

    public TidurService(SessionAccess access, JadwalTidurRepository repo) {
        this.access = java.util.Objects.requireNonNull(access);
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
        access.owner(penggunaId);
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

        JadwalTidur j = new JadwalTidur(0, access.owner(), tanggal, jamTidur, jamBangun, durasi, memenuhiStandar, pengingatAktif);
        int generatedId = access.call(() -> repo.simpan(j));
        if (generatedId <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
        j.setId(generatedId);
        return access.result(j);
    }

    public List<JadwalTidur> getRiwayatTidur(int penggunaId, int limit) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> repo.getRiwayatByPengguna(penggunaId, limit)), JadwalTidur::getPenggunaId);
    }

    public JadwalTidur getJadwalTerbaru(int penggunaId) throws SQLException {
        access.owner(penggunaId);
        return access.row(access.call(() -> repo.getTerbaruByPengguna(penggunaId)), JadwalTidur::getPenggunaId);
    }

    public boolean updatePengingat(int id, boolean aktif) throws SQLException {
        return access.call(() -> repo.setPengingatAktif(id, aktif, access.owner()));
    }

    public boolean hapusCatatan(int id) throws SQLException {
        return access.call(() -> repo.hapus(id, access.owner()));
    }
    /** Sleep is a useful independent record; evaluation is a separately committed follow-up. */
    public com.sehatharian.storage.WriteResult<JadwalTidur> catatDenganEvaluasi(int penggunaId,
            LocalDate tanggal, LocalTime jamTidur, LocalTime jamBangun, boolean pengingatAktif,
            EvaluasiService evaluasi) {
        access.owner(penggunaId);
        var primary = com.sehatharian.storage.WriteResult.attempt(() ->
            catatJamTidur(penggunaId, tanggal, jamTidur, jamBangun, pengingatAktif));
        access.owner();
        if (!primary.confirmed() || primary.value().isMemenuhiStandar()) return primary;
        com.sehatharian.storage.WriteResult<?> followup;
        try {
            followup = com.sehatharian.storage.WriteResult.attempt(() -> evaluasi.buatEvaluasiMingguan(penggunaId, tanggal));
        } catch (IllegalStateException insufficientData) {
            // Existing evaluation data-eligibility rule: no evaluation write has been attempted.
            followup = com.sehatharian.storage.WriteResult.failed();
        }
        access.owner();
        var steps = java.util.List.of(
            new com.sehatharian.storage.WriteResult.Step("Catatan tidur", primary.status()),
            new com.sehatharian.storage.WriteResult.Step("Evaluasi mingguan", followup.status()));
        return access.result(new com.sehatharian.storage.WriteResult<>(followup.confirmed()
            ? com.sehatharian.storage.WriteResult.Status.BERHASIL : com.sehatharian.storage.WriteResult.Status.PARSIAL,
            primary.value(), steps));
    }
}
