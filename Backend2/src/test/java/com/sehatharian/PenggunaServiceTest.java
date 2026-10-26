package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.PenggunaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PenggunaServiceTest {

    private PenggunaService penggunaService;

    @BeforeEach
    void setUp() {
        penggunaService = new PenggunaService();
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Penyelesaian onboarding menyimpan waktu luang, kesibukan, dan usia secara valid")
    void testCompleteOnboarding() throws SQLException {
        Pengguna user = penggunaService.getPenggunaAktif();
        assertNotNull(user);

        penggunaService.simpanOnboarding(user.getId(), 2, "2 jam", "INDOOR", "LANSIA");

        UserSession session = UserSession.getInstance();
        assertTrue(session.isOnboardingSelesai());
        assertEquals(2, session.getWaktuLuangHours());
        assertEquals("INDOOR", session.getJenisAktivitas());
        assertEquals("LANSIA", session.getKelompokUmur());

        // Verifikasi pada Pengguna aktif
        Pengguna updated = penggunaService.getPenggunaAktif();
        assertEquals(120, updated.getWaktuLuangMenit());
        assertEquals("dalam_rumah", updated.getGolonganKesibukan());
        assertEquals("lansia", updated.getGolonganUsia());
        assertTrue(updated.isOnboardingSelesai());
    }

    @Test
    @DisplayName("Batas waktu luang onboarding dibatasi maksimal 4 jam")
    void testOnboardingWaktuLuangMax4Hours() throws SQLException {
        Pengguna user = penggunaService.getPenggunaAktif();
        penggunaService.simpanOnboarding(user.getId(), 6, "6 jam", "OUTDOOR", "DEWASA");

        UserSession session = UserSession.getInstance();
        assertEquals(4, session.getWaktuLuangHours(), "Waktu luang maksimum yang diizinkan adalah 4 jam");
        assertEquals("OUTDOOR", session.getJenisAktivitas());
    }

    @Test
    @DisplayName("Pembaruan profil berhasil dengan nilai valid")
    void testUpdateProfilSuccess() throws SQLException {
        Pengguna user = penggunaService.getPenggunaAktif();
        boolean ok = penggunaService.updateProfil(
            user.getId(),
            "Nama Diperbarui",
            LocalDate.of(1995, 8, 20),
            60,
            "dewasa",
            "luar_rumah"
        );

        assertTrue(ok);
        Pengguna current = penggunaService.getPenggunaAktif();
        assertEquals("Nama Diperbarui", current.getNama());
        assertEquals(60, current.getWaktuLuangMenit());
    }

    @Test
    @DisplayName("Pembaruan profil gagal saat nama kosong atau waktu luang di luar batas")
    void testUpdateProfilValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            penggunaService.updateProfil(1, "", LocalDate.now(), 30, "dewasa", "luar_rumah")
        );
        assertThrows(IllegalArgumentException.class, () ->
            penggunaService.updateProfil(1, "Nama Valid", LocalDate.now(), 0, "dewasa", "luar_rumah")
        );
        assertThrows(IllegalArgumentException.class, () ->
            penggunaService.updateProfil(1, "Nama Valid", LocalDate.now(), 1000, "dewasa", "luar_rumah")
        );
    }
}
