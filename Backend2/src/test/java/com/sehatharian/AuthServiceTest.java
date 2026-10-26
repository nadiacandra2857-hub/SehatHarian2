package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Registrasi berhasil dengan data valid dan password terenkripsi BCrypt")
    void testRegisterSuccess() throws SQLException {
        String uniqueEmail = "user_" + System.currentTimeMillis() + "@sehatharian.test";
        Pengguna user = authService.register(
            "Budi Santoso",
            uniqueEmail,
            "password123",
            "password123",
            true
        );

        assertNotNull(user, "Pengguna baru harus berhasil dibuat");
        assertTrue(user.getId() > 0, "ID pengguna harus bernilai positif");
        assertEquals("Budi Santoso", user.getNama());
        assertEquals(uniqueEmail, user.getEmail());
        assertFalse(user.isOnboardingSelesai(), "Akun baru harus belum menyelesaikan onboarding");
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"),
                "Kata sandi harus di-hash dengan BCrypt");

        // Periksa UserSession
        UserSession session = UserSession.getInstance();
        assertTrue(session.isLoggedIn(), "Sesi pengguna harus berstatus login");
        assertEquals(user.getId(), session.getUserId());
        assertFalse(session.isOnboardingSelesai(), "Status sesi onboarding harus belum selesai");
    }

    @Test
    @DisplayName("Registrasi gagal saat syarat & ketentuan tidak disetujui")
    void testRegisterTermsNotAgreed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("Budi Santoso", "budi@test.com", "secret123", "secret123", false)
        );
        assertTrue(ex.getMessage().contains("syarat & ketentuan"));
    }

    @Test
    @DisplayName("Registrasi gagal saat nama kurang dari 3 karakter")
    void testRegisterInvalidName() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("Al", "ali@test.com", "secret123", "secret123", true)
        );
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("   ", "ali@test.com", "secret123", "secret123", true)
        );
    }

    @Test
    @DisplayName("Registrasi gagal saat format email tidak valid")
    void testRegisterInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("Ahmad Fauzi", "ahmadfauzi.com", "secret123", "secret123", true)
        );
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("Ahmad Fauzi", "ahmad@", "secret123", "secret123", true)
        );
    }

    @Test
    @DisplayName("Registrasi gagal saat kata sandi kurang dari 6 karakter")
    void testRegisterShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("Siti Aminah", "siti@test.com", "12345", "12345", true)
        );
    }

    @Test
    @DisplayName("Registrasi gagal saat konfirmasi kata sandi tidak cocok")
    void testRegisterMismatchedPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("Siti Aminah", "siti@test.com", "password123", "different123", true)
        );
    }

    @Test
    @DisplayName("Registrasi gagal saat email sudah terdaftar (Pemeriksaan duplikat)")
    void testRegisterDuplicateEmail() throws SQLException {
        String email = "duplikat_" + System.currentTimeMillis() + "@sehatharian.test";

        // Registrasi pertama berhasil
        authService.register("User Pertama", email, "password123", "password123", true);

        // Registrasi kedua dengan email yang sama harus gagal
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            authService.register("User Kedua", email, "password123", "password123", true)
        );
        assertTrue(ex.getMessage().contains("sudah terdaftar"));
    }

    @Test
    @DisplayName("Login berhasil dengan kredensial yang tepat")
    void testLoginSuccess() throws SQLException {
        String email = "login_" + System.currentTimeMillis() + "@sehatharian.test";
        authService.register("Rina Melati", email, "password123", "password123", true);
        UserSession.getInstance().logout();

        Pengguna loggedInUser = authService.login(email, "password123");
        assertNotNull(loggedInUser);
        assertEquals("Rina Melati", loggedInUser.getNama());
        assertTrue(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("Login gagal saat kata sandi salah")
    void testLoginWrongPassword() throws SQLException {
        String email = "wrongpass_" + System.currentTimeMillis() + "@sehatharian.test";
        authService.register("Dewi Lestari", email, "password123", "password123", true);
        UserSession.getInstance().logout();

        SecurityException ex = assertThrows(SecurityException.class, () ->
            authService.login(email, "passwordSalah")
        );
        assertTrue(ex.getMessage().contains("Kata sandi yang Anda masukkan salah"));
    }

    @Test
    @DisplayName("Login gagal saat akun tidak ditemukan")
    void testLoginUserNotFound() {
        SecurityException ex = assertThrows(SecurityException.class, () ->
            authService.login("nonexistent_" + System.currentTimeMillis() + "@sehatharian.test", "password123")
        );
        assertTrue(ex.getMessage().contains("tidak ditemukan"));
    }

    @Test
    @DisplayName("Logout membersihkan seluruh status sesi pengguna")
    void testLogoutClearsSession() throws SQLException {
        String email = "logout_" + System.currentTimeMillis() + "@sehatharian.test";
        authService.register("User Logout", email, "password123", "password123", true);
        assertTrue(UserSession.getInstance().isLoggedIn());

        authService.logout();
        assertFalse(UserSession.getInstance().isLoggedIn());
        assertEquals("", UserSession.getInstance().getUserEmail());
        assertFalse(UserSession.getInstance().isOnboardingSelesai());
    }
}
