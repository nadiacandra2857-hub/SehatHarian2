package com.sehatharian.service;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * PenggunaService mengelola profil pengguna, preferensi kesehatan,
 * dan penyelesaian alur onboarding.
 */
public class PenggunaService {

    private final PenggunaRepository penggunaRepo;

    public PenggunaService() {
        this.penggunaRepo = new PenggunaRepository();
    }

    public PenggunaService(PenggunaRepository penggunaRepo) {
        this.penggunaRepo = penggunaRepo;
    }

    public Pengguna getPenggunaAktif() throws SQLException {
        UserSession session = UserSession.getInstance();
        if (session.getActiveUser() != null) {
            return session.getActiveUser();
        }
        Pengguna p = penggunaRepo.getDefaultPengguna();
        if (p != null) {
            session.setCurrentUser(p);
        }
        return p;
    }

    public boolean updateProfil(int id, String nama, LocalDate tanggalLahir, int waktuLuangMenit,
                                String golonganUsia, String golonganKesibukan) throws SQLException, IllegalArgumentException {
        if (!ValidationUtil.isValidNama(nama)) {
            throw new IllegalArgumentException("Nama lengkap tidak boleh kosong (minimal 3 karakter).");
        }
        if (!ValidationUtil.isValidWaktuLuangMenit(waktuLuangMenit)) {
            throw new IllegalArgumentException("Waktu luang harus antara 1 sampai 480 menit.");
        }

        String usia = "lansia".equalsIgnoreCase(golonganUsia) ? "lansia" : "dewasa";
        String kesibukan = "dalam_rumah".equalsIgnoreCase(golonganKesibukan) ? "dalam_rumah" : "luar_rumah";

        Pengguna p = penggunaRepo.getById(id);
        if (p == null) {
            p = penggunaRepo.getDefaultPengguna();
        }
        if (p != null) {
            p.setNama(nama.trim());
            p.setTanggalLahir(tanggalLahir);
            p.setWaktuLuangMenit(waktuLuangMenit);
            p.setGolonganUsia(usia);
            p.setGolonganKesibukan(kesibukan);

            boolean success = penggunaRepo.updatePengguna(p);
            if (success) {
                UserSession.getInstance().setCurrentUser(p);
            }
            return success;
        }
        return false;
    }

    public void simpanOnboarding(int penggunaId, int waktuLuangHours, String waktuLuangLabel,
                                 String jenisAktivitas, String kelompokUmur) throws SQLException {
        int boundedHours = Math.min(Math.max(waktuLuangHours, 0), 4);
        int durasiMenit = (boundedHours == 0) ? 45 : (boundedHours * 60);

        String kesibukan = "INDOOR".equalsIgnoreCase(jenisAktivitas) ? "dalam_rumah" : "luar_rumah";
        String usia = "LANSIA".equalsIgnoreCase(kelompokUmur) ? "lansia" : "dewasa";

        penggunaRepo.updateOnboarding(penggunaId, durasiMenit, kesibukan, usia, true);

        // Update UserSession
        UserSession session = UserSession.getInstance();
        session.completeOnboarding(boundedHours, waktuLuangLabel, jenisAktivitas, kelompokUmur);
    }
}
