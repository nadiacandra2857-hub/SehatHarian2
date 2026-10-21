package com.sehatharian.service;

import com.sehatharian.model.Rekomendasi;
import com.sehatharian.repository.RekomendasiRepository;

import java.sql.SQLException;
import java.util.List;

public class RekomendasiService {
    private final RekomendasiRepository repo = new RekomendasiRepository();

    /**
     * F-02: Mendapatkan rekomendasi aktivitas berdasarkan waktu luang yang dimiliki pengguna.
     * Tidak menyajikan rekomendasi yang durasinya melebihi waktu luang.
     */
    public List<Rekomendasi> getAktivitasSesuaiWaktuLuang(int waktuLuangMenit, String kesibukan, String usia) throws SQLException {
        int maxDurasi = waktuLuangMenit > 0 ? waktuLuangMenit : 30;
        return repo.getFiltered("aktivitas", kesibukan, usia, maxDurasi);
    }

    /**
     * F-06: Filter rekomendasi Menu dan Workout berdasarkan golongan kesibukan dan usia.
     */
    public List<Rekomendasi> getRekomendasiMenuDanWorkout(String tipe, String kesibukan, String usia) throws SQLException {
        return repo.getFiltered(tipe, kesibukan, usia, 0);
    }

    public List<Rekomendasi> getAllRekomendasi() throws SQLException {
        return repo.getAll();
    }
}
