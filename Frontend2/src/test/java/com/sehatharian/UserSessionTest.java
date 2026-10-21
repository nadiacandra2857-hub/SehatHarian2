package com.sehatharian;

import com.sehatharian.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian State UserSession & Alur Onboarding Frontend")
class UserSessionTest {

    private UserSession session;

    @BeforeEach
    void setUp() {
        session = UserSession.getInstance();
        session.logout();
    }

    @Test
    @DisplayName("Pendaftaran akun baru harus menyetel onboardingSelesai = false")
    void testRegisterNewAccountRequiresOnboarding() {
        session.registerNewAccount("Nadia Candra", "nadia@sehatharian.id");

        assertTrue(session.isLoggedIn(), "Pengguna harus berstatus login setelah register");
        assertEquals("Nadia Candra", session.getUserName());
        assertEquals("nadia@sehatharian.id", session.getUserEmail());
        assertFalse(session.isOnboardingSelesai(), "Akun baru belum boleh ditandai selesai onboarding");
    }

    @Test
    @DisplayName("Menyelesaikan 3 slide onboarding harus menyetel onboardingSelesai = true")
    void testCompleteOnboarding() {
        session.registerNewAccount("Adhitya Ramadhan", "adhitya@sehatharian.id");
        assertFalse(session.isOnboardingSelesai());

        // Pengguna menyelesaikan 3 slide: 2 jam, OUTDOOR, DEWASA
        session.completeOnboarding(2, "2 jam", "OUTDOOR", "DEWASA");

        assertTrue(session.isOnboardingSelesai(), "Onboarding harus ditandai selesai");
        assertEquals(2, session.getWaktuLuangHours());
        assertEquals("2 jam", session.getWaktuLuangLabel());
        assertEquals("OUTDOOR", session.getJenisAktivitas());
        assertEquals("DEWASA", session.getKelompokUmur());
    }

    @Test
    @DisplayName("Waktu luang tidak boleh melebihi batas maksimal 4 jam")
    void testWaktuLuangMaximumFourHours() {
        // Jika dicoba mengisi nilai > 4 jam, harus di-clamp maksimal 4
        session.completeOnboarding(5, "5 jam", "INDOOR", "LANSIA");
        assertEquals(4, session.getWaktuLuangHours(), "Waktu luang maksimal harus 4 jam sesuai PRD");

        // Dan waktu luang < 0 di-clamp ke 0 (kurang dari 1 jam)
        session.completeOnboarding(-1, "< 1 jam", "INDOOR", "LANSIA");
        assertEquals(0, session.getWaktuLuangHours());
    }

    @Test
    @DisplayName("Kelompok umur dan aktivitas harus ternormalisasi dengan benar")
    void testPreferenceNormalization() {
        session.completeOnboarding(1, "1 jam", "indoor", "lansia");
        assertEquals("INDOOR", session.getJenisAktivitas());
        assertEquals("LANSIA", session.getKelompokUmur());

        session.completeOnboarding(3, "3 jam", "outdoor", "remaja");
        assertEquals("OUTDOOR", session.getJenisAktivitas());
        assertEquals("REMAJA", session.getKelompokUmur());
    }

    @Test
    @DisplayName("Login pengguna yang sudah ada tidak memunculkan onboarding ulang")
    void testLoginExistingUserDoesNotRequireOnboarding() {
        session.login("nadia@sehatharian.id", "Nadia Ayu");
        session.setOnboardingSelesai(true);

        assertTrue(session.isLoggedIn());
        assertTrue(session.isOnboardingSelesai(), "Pengguna yang login langsung ke Dashboard tanpa onboarding ulang");
    }

    @Test
    @DisplayName("Logout membersihkan sesi pengguna dengan aman")
    void testLogoutClearsSession() {
        session.registerNewAccount("User Test", "test@example.com");
        session.completeOnboarding(1, "1 jam", "OUTDOOR", "DEWASA");
        assertTrue(session.isLoggedIn());

        session.logout();
        assertFalse(session.isLoggedIn());
        assertFalse(session.isOnboardingSelesai());
    }
}
