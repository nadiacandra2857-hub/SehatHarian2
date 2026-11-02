package com.sehatharian.service;

import com.sehatharian.model.SessionAccess;
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
    private final UserSession session;
    private SessionAccess boundAccess;

    public PenggunaService(PenggunaRepository penggunaRepo, UserSession session) {
        this.penggunaRepo = java.util.Objects.requireNonNull(penggunaRepo);
        this.session = java.util.Objects.requireNonNull(session);
    }

    public PenggunaService forView(SessionAccess access) {
        // A scope from another session cannot replace this service's verified identity.
        access.requireSession(session);
        if (access.profile().getId() != session.requireActiveUser().getId()) throw new SecurityException("Data tidak tersedia.");
        PenggunaService view = new PenggunaService(penggunaRepo, session);
        view.boundAccess = access;
        return view;
    }
    private SessionAccess scope() {
        return boundAccess != null ? boundAccess : new SessionAccess(session);
    }

    public Pengguna getPenggunaAktif() throws SQLException {
        if (boundAccess != null) return boundAccess.profile();
        return session.getActiveUser();
    }

    public boolean updateProfil(int id, String nama, LocalDate tanggalLahir, int waktuLuangMenit,
                                String golonganUsia, String golonganKesibukan) throws SQLException, IllegalArgumentException {
        var access = scope();
        access.owner(id);
        if (!ValidationUtil.isValidNama(nama)) {
            throw new IllegalArgumentException("Nama lengkap tidak boleh kosong (minimal 3 karakter).");
        }
        if (!ValidationUtil.isValidWaktuLuangMenit(waktuLuangMenit)) {
            throw new IllegalArgumentException("Waktu luang harus antara 1 sampai 480 menit.");
        }

        String usia = "lansia".equalsIgnoreCase(golonganUsia) ? "lansia" : "dewasa";
        String kesibukan = "dalam_rumah".equalsIgnoreCase(golonganKesibukan) ? "dalam_rumah" : "luar_rumah";

        Pengguna p = getPenggunaAktif();
        if (p == null || p.getId() != id) throw new SecurityException("Silakan masuk kembali.");
        p.setNama(nama.trim());
        p.setTanggalLahir(tanggalLahir);
        p.setWaktuLuangMenit(waktuLuangMenit);
        p.setGolonganUsia(usia);
        p.setGolonganKesibukan(kesibukan);

        boolean success = access.call(() -> penggunaRepo.updatePengguna(p, access));
        if (success) {
            access.apply(() -> session.refreshCurrentUser(p));
        }
        return success;
    }

    public void simpanOnboarding(int penggunaId, int waktuLuangHours, String waktuLuangLabel,
                                 String jenisAktivitas, String kelompokUmur) throws SQLException {
        var access = scope();
        access.owner(penggunaId);
        Pengguna active = getPenggunaAktif();
        if (active == null || active.getId() != penggunaId) throw new SecurityException("Silakan masuk kembali.");
        int boundedHours = Math.min(Math.max(waktuLuangHours, 0), 4);
        int durasiMenit = (boundedHours == 0) ? 45 : (boundedHours * 60);

        String kesibukan = "INDOOR".equalsIgnoreCase(jenisAktivitas) ? "dalam_rumah" : "luar_rumah";
        String usia = "LANSIA".equalsIgnoreCase(kelompokUmur) ? "lansia" : "dewasa";

        if (!access.call(() -> penggunaRepo.updateOnboarding(penggunaId, durasiMenit, kesibukan, usia, true, access))) {
            throw new SQLException("Profil belum diperbarui.");
        }

        // Update UserSession
        access.apply(() -> session.completeOnboarding(boundedHours, waktuLuangLabel, jenisAktivitas, kelompokUmur));
    }
    /** Existing password-write boundary, no new reset/remember-me flow. */
    public boolean updatePassword(int id, String newPasswordHash) throws SQLException {
        var access = scope();
        access.owner(id);
        if (newPasswordHash == null || newPasswordHash.isBlank()) throw new IllegalArgumentException("Hash wajib diisi.");
        boolean success = access.call(() -> penggunaRepo.updatePassword(id, newPasswordHash, access));
        if (success) {
            Pengguna profile = access.profile();
            profile.setPasswordHash(newPasswordHash);
            access.apply(() -> session.refreshCurrentUser(profile));
        }
        return success;
    }
    /** Ordinary authorized SQL read, not proof of which request wrote the observed values. */
    public Pengguna muatProfilTersimpan() throws SQLException {
        var access = scope();
        Pengguna stored = access.row(access.call(() -> penggunaRepo.getById(access.owner(), access)), Pengguna::getId);
        if (stored != null) access.apply(() -> session.refreshCurrentUser(stored));
        return stored;
    }
}
