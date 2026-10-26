package com.sehatharian.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateHelper {

    private static final Locale LOCALE_ID = Locale.of("id", "ID");
    private static final DateTimeFormatter FORMAT_TANGGAL_LENGKAP = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LOCALE_ID);
    private static final DateTimeFormatter FORMAT_TANGGAL_PENDEK = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_ID);
    private static final DateTimeFormatter FORMAT_JAM_MENIT = DateTimeFormatter.ofPattern("HH:mm", LOCALE_ID);

    public static String formatTanggalLengkap(LocalDate date) {
        return date != null ? date.format(FORMAT_TANGGAL_LENGKAP) : "-";
    }

    public static String formatTanggalPendek(LocalDate date) {
        return date != null ? date.format(FORMAT_TANGGAL_PENDEK) : "-";
    }

    public static String formatJamMenit(LocalTime time) {
        return time != null ? time.format(FORMAT_JAM_MENIT) : "--:--";
    }

    public static String getSalamWaktu(LocalTime time) {
        if (time == null) return "Halo";
        int hour = time.getHour();
        if (hour >= 4 && hour < 11) return "Selamat Pagi";
        if (hour >= 11 && hour < 15) return "Selamat Siang";
        if (hour >= 15 && hour < 18) return "Selamat Sore";
        return "Selamat Malam";
    }

    public static String getMotivasiHarian(LocalTime time) {
        int hour = time != null ? time.getHour() : 10;
        if (hour < 11) {
            return "\"Mulailah hari ini dengan satu pilihan sehat kecil: air putih dan peregangan ringan.\"";
        } else if (hour < 17) {
            return "\"Konsistensi bukan tentang sempurna, melainkan tentang tidak berhenti mencoba setiap hari.\"";
        } else {
            return "\"Istirahatkan tubuh dan pikiran malam ini. Tidur berkualitas adalah fondasi kesehatan esok hari.\"";
        }
    }
}
