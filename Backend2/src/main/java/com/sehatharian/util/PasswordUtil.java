package com.sehatharian.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil menangani hashing dan verifikasi kata sandi secara aman
 * menggunakan algoritma BCrypt dengan salt dinamis (cost factor 12).
 * Sesuai panduan keamanan PRD Bagian 4.A: Dilarang menyimpan kata sandi dalam teks biasa
 * atau sekadar MD5/SHA-256 tanpa salt.
 */
public class PasswordUtil {

    private static final int BCRYPT_WORK_FACTOR = 12;

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
        if (!hashedPassword.startsWith("$2a$") && !hashedPassword.startsWith("$2b$") && !hashedPassword.startsWith("$2y$")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
