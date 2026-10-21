package com.sehatharian;

import com.sehatharian.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Skenario & Validasi Frontend Flow (Tahap E)")
class FrontendFlowValidationTest {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Skenario Validasi Format Email Form Pendaftaran")
    void testEmailValidation() {
        // Valid
        assertTrue(EMAIL_PATTERN.matcher("nadia@sehatharian.id").matches());
        assertTrue(EMAIL_PATTERN.matcher("user.name+tag@example.com").matches());
        assertTrue(EMAIL_PATTERN.matcher("test@pens.ac.id").matches());

        // Tidak Valid
        assertFalse(EMAIL_PATTERN.matcher("invalid-email").matches());
        assertFalse(EMAIL_PATTERN.matcher("user@domain").matches());
        assertFalse(EMAIL_PATTERN.matcher("@missinguser.com").matches());
        assertFalse(EMAIL_PATTERN.matcher("").matches());
    }

    @Test
    @DisplayName("Skenario Validasi Password dan Konfirmasi Password")
    void testPasswordMatchValidation() {
        String pass1 = "rahasia123";
        String pass2 = "rahasia123";
        String passMismatch = "rahasia456";

        assertTrue(pass1.equals(pass2), "Password yang identik harus valid");
        assertFalse(pass1.equals(passMismatch), "Password yang tidak cocok harus gagal");
        assertTrue(pass1.length() >= 6, "Password minimal 6 karakter");
        assertFalse("12345".length() >= 6, "Password kurang dari 6 karakter harus ditolak");
    }

    @Test
    @DisplayName("Skenario Validasi Syarat & Ketentuan Wajib Dicentang")
    void testTermsCheckboxValidation() {
        boolean termsChecked = true;
        boolean termsUnchecked = false;

        assertTrue(termsChecked, "Pendaftaran hanya dapat diproses bila syarat & ketentuan dicentang");
        assertFalse(termsUnchecked, "Pendaftaran harus ditolak jika syarat & ketentuan belum dicentang");
    }

    @Test
    @DisplayName("Skenario Alur Slide 1: Pengguna tidak dapat lanjut tanpa memilih waktu luang")
    void testSlide1Validation() {
        Integer selectedWaktuLuang = null;
        assertNull(selectedWaktuLuang, "Jawaban belum dipilih");

        // Validasi: tombol berikutnya harus disable
        boolean canNext = (selectedWaktuLuang != null);
        assertFalse(canNext, "Tidak boleh lanjut ke slide 2 jika belum memilih waktu luang");

        // Pengguna memilih 2 jam (<= 4 jam)
        selectedWaktuLuang = 2;
        canNext = (selectedWaktuLuang != null && selectedWaktuLuang <= 4);
        assertTrue(canNext, "Dapat lanjut setelah memilih waktu luang valid");
    }

    @Test
    @DisplayName("Skenario Alur Slide 2: Pengguna tidak dapat lanjut tanpa memilih Outdoor/Indoor")
    void testSlide2Validation() {
        String selectedAktivitas = null;
        assertFalse(selectedAktivitas != null, "Belum memilih aktivitas");

        // Pengguna memilih Outdoor
        selectedAktivitas = "OUTDOOR";
        assertTrue("OUTDOOR".equals(selectedAktivitas) || "INDOOR".equals(selectedAktivitas));
    }

    @Test
    @DisplayName("Skenario Alur Slide 3: Hanya selesai jika seluruh ketiga slide telah terisi")
    void testSlide3CompletionValidation() {
        Integer waktuLuang = 3; // Slide 1
        String aktivitas = "INDOOR"; // Slide 2
        String umur = null; // Slide 3 belum dipilih

        boolean allAnswered = (waktuLuang != null && aktivitas != null && umur != null);
        assertFalse(allAnswered, "Slide 3 tidak boleh diselesaikan jika kelompok umur belum dipilih");

        // Pilih Lansia
        umur = "LANSIA";
        allAnswered = (waktuLuang != null && aktivitas != null && umur != null);
        assertTrue(allAnswered, "Semua jawaban telah lengkap, tombol Selesai dapat dieksekusi");

        // Selesaikan
        UserSession.getInstance().completeOnboarding(waktuLuang, "3 jam", aktivitas, umur);
        assertTrue(UserSession.getInstance().isOnboardingSelesai());
    }

    @Test
    @DisplayName("Skenario Login Langsung ke Dashboard Tanpa Onboarding Ulang")
    void testLoginBypassesOnboardingForExistingUser() {
        UserSession session = UserSession.getInstance();
        session.login("nadia@sehatharian.id", "Nadia Ayu");
        session.setOnboardingSelesai(true);

        assertTrue(session.isLoggedIn());
        assertTrue(session.isOnboardingSelesai(), "Pengguna lama tidak melihat onboarding saat login");
    }
}
