package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.PenggunaService;
import com.sehatharian.service.AuthService;
import com.sehatharian.repository.PenggunaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PenggunaServiceTest {

    private PenggunaService penggunaService;
    private final UserSession session = new UserSession();

    @BeforeEach
    void setUp() throws SQLException {
        session.logout();
        new AuthService(new AuthFixtures.Accounts(), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        penggunaService = new PenggunaService(new PenggunaRepository(() -> {
            throw new AssertionError("Tests must not open a database connection");
        }) {
            @Override public Pengguna getById(int id, com.sehatharian.model.SessionAccess access) { throw new AssertionError("Active identity comes from verified session"); }
            @Override public boolean updatePengguna(Pengguna p, com.sehatharian.model.SessionAccess access) { return true; }
            @Override public boolean updateOnboarding(int id, int minutes, String context, String age, boolean done, com.sehatharian.model.SessionAccess access) { return true; }
        }, session);
    }

    @AfterEach void tearDown() { session.logout(); }

    @Test void noSessionCannotBeRecoveredByProfileUpdateOrOnboarding() throws SQLException {
        session.logout();
        assertNull(penggunaService.getPenggunaAktif());
        assertThrows(SecurityException.class, () -> penggunaService.updateProfil(37, "Valid User", null, 30, "dewasa", "luar_rumah"));
        assertThrows(SecurityException.class, () -> penggunaService.simpanOnboarding(37, 1, "1 jam", "INDOOR", "DEWASA"));
        assertFalse(session.isLoggedIn());
    }

    @Test
    @DisplayName("Penyelesaian onboarding menyimpan waktu luang, kesibukan, dan usia secara valid")
    void testCompleteOnboarding() throws SQLException {
        Pengguna user = penggunaService.getPenggunaAktif();
        assertNotNull(user);

        penggunaService.simpanOnboarding(user.getId(), 2, "2 jam", "INDOOR", "LANSIA");

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
            penggunaService.updateProfil(session.getUserId(), "", LocalDate.now(), 30, "dewasa", "luar_rumah")
        );
        assertThrows(IllegalArgumentException.class, () ->
            penggunaService.updateProfil(session.getUserId(), "Nama Valid", LocalDate.now(), 0, "dewasa", "luar_rumah")
        );
        assertThrows(IllegalArgumentException.class, () ->
            penggunaService.updateProfil(session.getUserId(), "Nama Valid", LocalDate.now(), 1000, "dewasa", "luar_rumah")
        );
    }
}
