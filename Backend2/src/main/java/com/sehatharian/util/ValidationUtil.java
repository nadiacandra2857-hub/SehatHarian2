package com.sehatharian.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil menyediakan validasi terpusat untuk input pengguna
 * pada lapisan service dan domain SehatHarian.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return !trimmed.isEmpty() && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidNama(String nama) {
        return nama != null && nama.trim().length() >= 3;
    }

    public static boolean isValidWaktuLuangMenit(int menit) {
        return menit >= 1 && menit <= 480;
    }

    public static boolean isValidDurasiAktivitas(int menit) {
        return menit >= 1 && menit <= 720;
    }

    public static boolean isValidJumlahHidrasiMl(int ml) {
        return ml >= 10 && ml <= 5000;
    }

    public static boolean isValidWaktuMakan(String waktuMakan) {
        if (waktuMakan == null) return false;
        String val = waktuMakan.trim().toLowerCase();
        return val.equals("sarapan") || val.equals("makan_siang") || 
               val.equals("makan_malam") || val.equals("camilan");
    }

    public static boolean isValidGolonganUsia(String usia) {
        if (usia == null) return false;
        String val = usia.trim().toLowerCase();
        return val.equals("dewasa") || val.equals("lansia");
    }

    public static boolean isValidGolonganKesibukan(String kesibukan) {
        if (kesibukan == null) return false;
        String val = kesibukan.trim().toLowerCase();
        return val.equals("luar_rumah") || val.equals("dalam_rumah");
    }

    public static boolean isValidStatusTantangan(String status) {
        if (status == null) return false;
        String val = status.trim().toLowerCase();
        return val.equals("disarankan") || val.equals("selesai") || val.equals("dilewati");
    }
}
