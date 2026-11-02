package com.sehatharian.service;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RiwayatTantangan;
import com.sehatharian.model.Tantangan;
import com.sehatharian.repository.RiwayatTantanganRepository;
import com.sehatharian.repository.TantanganRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TantanganService {
    private final SessionAccess access;
    private final TantanganRepository tantanganRepo;
    private final RiwayatTantanganRepository riwayatRepo;

    public TantanganService(SessionAccess access) {
        this.access = java.util.Objects.requireNonNull(access);
        this.tantanganRepo = new TantanganRepository();
        this.riwayatRepo = new RiwayatTantanganRepository(access);
    }

    public TantanganService(SessionAccess access, TantanganRepository tantanganRepo, RiwayatTantanganRepository riwayatRepo) {
        this.access = java.util.Objects.requireNonNull(access);
        this.tantanganRepo = tantanganRepo;
        this.riwayatRepo = riwayatRepo;
    }

    /**
     * Memastikan tantangan hari ini tersedia untuk pengguna.
     * Jika belum ada, sistem akan memilih 2-3 tantangan yang adaptif
     * sesuai tipe kesibukan, usia, dan waktu luang pengguna.
     */
    public List<RiwayatTantangan> pastikanTantanganHariIni(Pengguna pengguna, LocalDate tanggal) throws SQLException {
        if (pengguna == null) throw new IllegalArgumentException("Profil wajib diisi.");
        access.owner(pengguna.getId());
        final Pengguna trusted = access.profile();
        if (access.automaticChallengesBlocked(tanggal))
            return getTantanganByTanggal(trusted.getId(), tanggal); // read-only review; never retry the assignment
        java.util.List<com.sehatharian.storage.WriteResult.Step> steps = new java.util.ArrayList<>();
        try {

        List<RiwayatTantangan> existing = access.rows(access.call(() -> riwayatRepo.getByTanggal(trusted.getId(), tanggal)), RiwayatTantangan::getPenggunaId);
        if (!existing.isEmpty()) {
            return existing;
        }

        // Cari tantangan yang cocok dengan profil pengguna
        List<Tantangan> kandidat = access.call(() -> tantanganRepo.getByFilter(
            trusted.getGolonganKesibukan(),
            trusted.getGolonganUsia(),
            Math.max(trusted.getWaktuLuangMenit(), 15)
        ));

        if (kandidat.isEmpty()) {
            kandidat = access.call(() -> tantanganRepo.getAll());
        }

        // Ambil hingga 3 tantangan
        int count = Math.min(kandidat.size(), 3);
        for (int i = 0; i < count; i++) {
            Tantangan t = kandidat.get(i);
            if (!access.call(() -> riwayatRepo.existsForDate(trusted.getId(), t.getId(), tanggal))) {
                RiwayatTantangan rt = new RiwayatTantangan();
                rt.setPenggunaId(trusted.getId());
                rt.setTantanganId(t.getId());
                rt.setTanggal(tanggal);
                rt.setDurasiTargetMenit(t.getDurasiMenit());
                rt.setStatus("disarankan");
                var result = com.sehatharian.storage.WriteResult.attempt(() -> {
                    int id = access.call(() -> riwayatRepo.tambah(rt));
                    if (id <= 0) throw new com.sehatharian.storage.WriteException(com.sehatharian.storage.WriteResult.failed());
                    return id;
                });
                if (!result.confirmed()) {
                    if (result.needsReview() || !steps.isEmpty()) access.blockAutomaticChallenges(tanggal);
                    steps.add(new com.sehatharian.storage.WriteResult.Step("Penugasan tantangan " + (i + 1), result.status()));
                    throw new com.sehatharian.storage.WriteException(new com.sehatharian.storage.WriteResult<>(
                        steps.stream().anyMatch(step -> step.status() == com.sehatharian.storage.WriteResult.Status.BERHASIL)
                            ? com.sehatharian.storage.WriteResult.Status.PARSIAL : result.status(), null, steps));
                }
                steps.add(new com.sehatharian.storage.WriteResult.Step("Penugasan tantangan " + (i + 1), result.status()));
            }
        }

        return access.rows(access.call(() -> riwayatRepo.getByTanggal(trusted.getId(), tanggal)), RiwayatTantangan::getPenggunaId);
        } catch (com.sehatharian.storage.WriteException e) { throw e; }
        catch (SQLException e) {
            if (steps.isEmpty()) throw e; // a read failed before any assignment was committed
            access.blockAutomaticChallenges(tanggal);
            steps.add(new com.sehatharian.storage.WriteResult.Step("Pemuatan hasil", com.sehatharian.storage.WriteResult.Status.GAGAL));
            throw new com.sehatharian.storage.WriteException(new com.sehatharian.storage.WriteResult<>(
                com.sehatharian.storage.WriteResult.Status.PARSIAL, null, steps));
        }
    }

    public List<RiwayatTantangan> getTantanganByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> riwayatRepo.getByTanggal(penggunaId, tanggal)), RiwayatTantangan::getPenggunaId);
    }

    public boolean selesaikanTantangan(int riwayatId) throws SQLException {
        return access.call(() -> riwayatRepo.updateStatus(riwayatId, "selesai", access.owner()));
    }

    public boolean lewatiTantangan(int riwayatId) throws SQLException {
        return access.call(() -> riwayatRepo.updateStatus(riwayatId, "dilewati", access.owner()));
    }

    public boolean resetTantangan(int riwayatId) throws SQLException {
        return access.call(() -> riwayatRepo.updateStatus(riwayatId, "disarankan", access.owner()));
    }

    public boolean tambahTantanganKeHariIni(int penggunaId, int tantanganId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        if (access.call(() -> riwayatRepo.existsForDate(penggunaId, tantanganId, tanggal))) {
            return false; // Jangan duplikat tantangan yang sama pada hari yang sama
        }
        Tantangan t = access.call(() -> tantanganRepo.getById(tantanganId));
        if (t == null) return false;

        RiwayatTantangan rt = new RiwayatTantangan();
        rt.setPenggunaId(access.owner());
        rt.setTantanganId(tantanganId);
        rt.setTanggal(tanggal);
        rt.setDurasiTargetMenit(t.getDurasiMenit());
        rt.setStatus("disarankan");
        if (access.call(() -> riwayatRepo.tambah(rt)) <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
        return true;
    }

    public List<Tantangan> getAllKatalogTantangan() throws SQLException {
        return access.call(() -> tantanganRepo.getAll());
    }

    public boolean hapusTantanganPadaTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.call(() -> riwayatRepo.deleteByPenggunaAndTanggal(penggunaId, tanggal));
    }
}
