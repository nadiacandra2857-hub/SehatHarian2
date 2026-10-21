package com.sehatharian.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateHelper {
    private static final Locale INDO_LOCALE = Locale.of("id", "ID");
    private static final DateTimeFormatter TANGGAL_LENGKAP = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", INDO_LOCALE);
    private static final DateTimeFormatter TANGGAL_PENDEK = DateTimeFormatter.ofPattern("d MMM yyyy", INDO_LOCALE);
    private static final DateTimeFormatter JAM_MENIT = DateTimeFormatter.ofPattern("HH:mm");

    public static String formatTanggalLengkap(LocalDate date) {
        if (date == null) return "-";
        return date.format(TANGGAL_LENGKAP);
    }

    public static String formatTanggalPendek(LocalDate date) {
        if (date == null) return "-";
        return date.format(TANGGAL_PENDEK);
    }

    public static String formatJamMenit(LocalTime time) {
        if (time == null) return "-";
        return time.format(JAM_MENIT);
    }

    public static String getSalamWaktu(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 4 && hour < 11) return "Selamat Pagi";
        if (hour >= 11 && hour < 15) return "Selamat Siang";
        if (hour >= 15 && hour < 18) return "Selamat Sore";
        return "Selamat Malam";
    }

    public static String getMotivasiHarian(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 4 && hour < 11) {
            return "Awali hari dengan segelas air putih dan satu kebiasaan sehat kecil.";
        }
        if (hour >= 11 && hour < 15) {
            return "Tetap aktif di sela rutinitas. Luangkan 5 menit untuk peregangan ringan.";
        }
        if (hour >= 15 && hour < 18) {
            return "Segarkan pikiran setelah beraktivitas dengan jalan santai sore.";
        }
        return "Istirahat yang berkualitas adalah kunci pemulihan energi optimal esok hari.";
    }
}
