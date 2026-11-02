package com.sehatharian.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;

/**
 * PasswordUtil menangani hashing dan verifikasi kata sandi secara aman
 * menggunakan algoritma BCrypt dengan salt dinamis (cost factor 12).
 * Sesuai panduan keamanan PRD Bagian 4.A: Dilarang menyimpan kata sandi dalam teks biasa
 * atau sekadar MD5/SHA-256 tanpa salt.
 */
public class PasswordUtil {

    private static final int BCRYPT_WORK_FACTOR = 12;
    // jbcrypt 0.4 implements $2$ and $2a$, not $2b$ or $2y$.
    private static final Pattern SUPPORTED_HASH = Pattern.compile(
            "\\$2a?\\$(0[4-9]|[12][0-9]|30)\\$[./A-Za-z0-9]{53}");

    /**
     * Menghasilkan hash BCrypt aman dari kata sandi teks biasa.
     *
     * @param plainPassword Kata sandi teks biasa
     * @return String hash BCrypt 60 karakter
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Kata sandi tidak boleh kosong.");
        }
        String salt = BCrypt.gensalt(BCRYPT_WORK_FACTOR);
        return BCrypt.hashpw(plainPassword, salt);
    }

    /**
     * Memeriksa apakah kata sandi teks cocok dengan hash BCrypt yang tersimpan.
     *
     * @param plainPassword Kata sandi yang dimasukkan pengguna
     * @param hashedPassword Hash BCrypt dari database
     * @return true jika cocok, false jika tidak cocok atau format salah
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        if (!SUPPORTED_HASH.matcher(hashedPassword).matches()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
        }
    }
}
