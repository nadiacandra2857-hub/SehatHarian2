package com.sehatharian.service;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RiwayatTantangan;
import com.sehatharian.model.Tantangan;
import com.sehatharian.repository.RiwayatTantanganRepository;
import com.sehatharian.repository.TantanganRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TantanganService {
    private final TantanganRepository tantanganRepo;
    private final RiwayatTantanganRepository riwayatRepo;

    public TantanganService() {
        this.tantanganRepo = new TantanganRepository();
        this.riwayatRepo = new RiwayatTantanganRepository();
    }

    public TantanganService(TantanganRepository tantanganRepo, RiwayatTantanganRepository riwayatRepo) {
        this.tantanganRepo = tantanganRepo;
        this.riwayatRepo = riwayatRepo;
    }

    /**
     * Memastikan tantangan hari ini tersedia untuk pengguna.
     * Jika belum ada, sistem akan memilih 2-3 tantangan yang adaptif
     * sesuai tipe kesibukan, usia, dan waktu luang pengguna.
     */
    public List<RiwayatTantangan> pastikanTantanganHariIni(Pengguna pengguna, LocalDate tanggal) throws SQLException {
        if (pengguna == null) return List.of();

        List<RiwayatTantangan> existing = riwayatRepo.getByTanggal(pengguna.getId(), tanggal);
        if (!existing.isEmpty()) {
            return existing;
        }

        // Cari tantangan yang cocok dengan profil pengguna
        List<Tantangan> kandidat = tantanganRepo.getByFilter(
            pengguna.getGolonganKesibukan(),
            pengguna.getGolonganUsia(),
            Math.max(pengguna.getWaktuLuangMenit(), 15)
        );

        if (kandidat.isEmpty()) {
            kandidat = tantanganRepo.getAll();
        }

        // Ambil hingga 3 tantangan
        int count = Math.min(kandidat.size(), 3);
        for (int i = 0; i < count; i++) {
            Tantangan t = kandidat.get(i);
            if (!riwayatRepo.existsForDate(pengguna.getId(), t.getId(), tanggal)) {
                RiwayatTantangan rt = new RiwayatTantangan();
                rt.setPenggunaId(pengguna.getId());
                rt.setTantanganId(t.getId());
                rt.setTanggal(tanggal);
                rt.setDurasiTargetMenit(t.getDurasiMenit());
                rt.setStatus("disarankan");
                riwayatRepo.tambah(rt);
            }
        }

        return riwayatRepo.getByTanggal(pengguna.getId(), tanggal);
    }

    public List<RiwayatTantangan> getTantanganByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        return riwayatRepo.getByTanggal(penggunaId, tanggal);
    }

    public boolean selesaikanTantangan(int riwayatId) throws SQLException {
        return riwayatRepo.updateStatus(riwayatId, "selesai");
    }

    public boolean lewatiTantangan(int riwayatId) throws SQLException {
        return riwayatRepo.updateStatus(riwayatId, "dilewati");
    }

    public boolean resetTantangan(int riwayatId) throws SQLException {
        return riwayatRepo.updateStatus(riwayatId, "disarankan");
    }

    public boolean tambahTantanganKeHariIni(int penggunaId, int tantanganId, LocalDate tanggal) throws SQLException {
        if (riwayatRepo.existsForDate(penggunaId, tantanganId, tanggal)) {
            return false; // Jangan duplikat tantangan yang sama pada hari yang sama
        }
        Tantangan t = tantanganRepo.getById(tantanganId);
        if (t == null) return false;

        RiwayatTantangan rt = new RiwayatTantangan();
        rt.setPenggunaId(penggunaId);
        rt.setTantanganId(tantanganId);
        rt.setTanggal(tanggal);
        rt.setDurasiTargetMenit(t.getDurasiMenit());
        rt.setStatus("disarankan");
        riwayatRepo.tambah(rt);
        return true;
    }

    public List<Tantangan> getAllKatalogTantangan() throws SQLException {
        return tantanganRepo.getAll();
    }

    public boolean hapusTantanganPadaTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        return riwayatRepo.deleteByPenggunaAndTanggal(penggunaId, tanggal);
    }
}
