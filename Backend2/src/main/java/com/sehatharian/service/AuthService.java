package com.sehatharian.service;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.AccountRepository;
import java.util.Locale;
import java.util.Objects;
import com.sehatharian.util.PasswordUtil;
import com.sehatharian.util.ValidationUtil;

import java.sql.SQLException;

/**
 * AuthService mengelola alur pendaftaran akun, validasi kredensial login,
 * enkripsi kata sandi menggunakan BCrypt, dan inisialisasi sesi desktop.
 */
public class AuthService {

    public static final String INVALID_CREDENTIALS = "Email atau kata sandi tidak valid.";
    private final AccountRepository penggunaRepo;
    private final UserSession session;

    /** Only AuthService can produce this proof; UI/profile setters cannot authenticate. */
    public static final class VerifiedAccount {
        private final Pengguna pengguna;
        private VerifiedAccount(Pengguna pengguna) { this.pengguna = pengguna; }
        public Pengguna getPengguna() { return pengguna; }
    }

    public AuthService(AccountRepository penggunaRepo, UserSession session) {
        this.penggunaRepo = Objects.requireNonNull(penggunaRepo);
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Mendaftarkan akun pengguna baru dengan validasi ketat dan pemeriksaan email unik.
     *
     * @param nama Nama lengkap pengguna (min 3 karakter)
     * @param email Alamat email unik dan valid
     * @param password Kata sandi (min 6 karakter)
     * @param confirmPassword Konfirmasi kata sandi (harus cocok)
     * @param agreeTerms Persetujuan syarat dan ketentuan (harus true)
     * @return Pengguna yang berhasil didaftarkan
     * @throws IllegalArgumentException jika input tidak valid atau syarat tidak disetujui
     * @throws IllegalStateException jika email sudah terdaftar
     * @throws SQLException jika terjadi kegagalan penyimpanan pada database
     */
    public Pengguna register(String nama, String email, String password, String confirmPassword)
            throws IllegalArgumentException, IllegalStateException, SQLException {
        return register(nama, email, password, confirmPassword, true);
    }

    public Pengguna register(String nama, String email, String password, String confirmPassword, boolean agreeTerms)
            throws IllegalArgumentException, IllegalStateException, SQLException {
        long requestGeneration = session.getGeneration();

        // 1. Validasi Syarat & Ketentuan
        if (!agreeTerms) {
            throw new IllegalArgumentException("Anda harus menyetujui syarat & ketentuan untuk melanjutkan.");
        }

        // 2. Validasi Nama
        if (!ValidationUtil.isValidNama(nama)) {
            throw new IllegalArgumentException("Nama lengkap minimal 3 karakter dan tidak boleh kosong.");
        }

        // 3. Validasi Format Email
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Format email tidak valid (contoh: pengguna@domain.com).");
        }

        // 4. Validasi Kata Sandi
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Kata sandi minimal 6 karakter.");
        }

        // 5. Validasi Konfirmasi Kata Sandi
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Konfirmasi kata sandi tidak cocok dengan kata sandi.");
        }

        String emailTrimmed = email.trim().toLowerCase(Locale.ROOT);

        // 6. Pemeriksaan Email Duplikat
        if (penggunaRepo.existsByEmail(emailTrimmed)) {
            throw new IllegalStateException("Alamat email '" + emailTrimmed + "' sudah terdaftar. Silakan gunakan email lain atau masuk.");
        }

        // 7. Hashing Kata Sandi dengan BCrypt + Salt
        String passwordHash = PasswordUtil.hashPassword(password);

        // 8. Simpan ke database
        Pengguna baru = new Pengguna(
            0,
            nama.trim(),
            emailTrimmed,
            passwordHash,
            null,
            "dewasa",
            "luar_rumah",
            30,
            false // Onboarding belum selesai
        );

        int newId = penggunaRepo.createPengguna(baru);
        if (newId <= 0) {
            throw new SQLException("Pembuatan akun belum dikonfirmasi oleh penyimpanan.");
        }
        baru.setId(newId);

        // 9. Inisialisasi UserSession
        if (session.getGeneration() != requestGeneration) throw new SecurityException("Permintaan sesi sudah tidak berlaku.");
        session.authenticate(new VerifiedAccount(baru));
        session.setOnboardingSelesai(false);

        return baru;
    }

    /** Normal login supports the unique email identity only; passwords are never normalized. */
    public Pengguna login(String email, String password) throws SQLException {
        long requestGeneration = session.getGeneration();
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Masukkan alamat email yang valid.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Kata sandi wajib diisi.");
        }
        Pengguna pengguna = penggunaRepo.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (pengguna == null || pengguna.getId() <= 0
                || pengguna.getEmail() == null
                || !email.trim().equalsIgnoreCase(pengguna.getEmail())
                || !PasswordUtil.checkPassword(password, pengguna.getPasswordHash())) {
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        if (session.getGeneration() != requestGeneration) throw new SecurityException("Permintaan sesi sudah tidak berlaku.");
        session.authenticate(new VerifiedAccount(pengguna));
        return pengguna;
    }

    public void logout() {
        session.logout();
    }
}
