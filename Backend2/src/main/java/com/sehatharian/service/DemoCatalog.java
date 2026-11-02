package com.sehatharian.service;

import com.sehatharian.model.Rekomendasi;
import com.sehatharian.storage.ReadResult;
import java.util.ArrayList;
import java.util.List;

/** Explicit read-only preview. No account, personal history, SQL connection or writable repository. */
public final class DemoCatalog {
    private DemoCatalog() {}
    public static ReadResult<List<Rekomendasi>> recommendations(String type, String context, String age) {
        // New objects per read: editing a preview cannot change a later read or the SQL catalog.
        List<Rekomendasi> rows = new ArrayList<>();
        rows.add(new Rekomendasi(1, "aktivitas", "Pernapasan Diafragma 5 Menit", "Duduk tegak, tarik napas dalam 4 detik, tahan 4 detik, dan hembuskan perlahan 6 detik untuk menurunkan stres.", "semua", "semua", 5));
        rows.add(new Rekomendasi(2, "aktivitas", "Jalan Santai Sela Istirahat", "Manfaatkan jeda istirahat siang untuk berjalan santai 10 menit tanpa melihat layar gawai.", "luar_rumah", "dewasa", 10));
        rows.add(new Rekomendasi(3, "aktivitas", "Jalan Kaki Sore 25 Menit", "Berjalan santai mengelilingi perumahan sambil menikmati udara sore yang segar.", "dalam_rumah", "semua", 25));
        rows.add(new Rekomendasi(4, "aktivitas", "Peregangan Otot Kaki & Betis", "Regangkan otot betis dan paha belakang setelah duduk lama bekerja.", "semua", "dewasa", 15));
        rows.add(new Rekomendasi(5, "menu", "Sarapan Oatmeal Pisang & Madu", "Semangkuk oatmeal hangat dengan irisan pisang, taburan biji chia, dan satu sendok teh madu alami.", "dalam_rumah", "dewasa", 10));
        rows.add(new Rekomendasi(6, "menu", "Sup Bening Bayam, Jagung Manis & Tempe Kukus", "Menu kaya serat, rendah lemak jenuh, dan mudah dicerna untuk menjaga kestabilan tekanan darah.", "semua", "lansia", 20));
        rows.add(new Rekomendasi(7, "menu", "Kotak Bekal Sehat: Nasi Merah & Ayam Panggang", "Porsi seimbang dengan nasi merah, dada ayam panggang bumbu kunyit, serta tumis buncis wortel.", "luar_rumah", "dewasa", 25));
        rows.add(new Rekomendasi(8, "menu", "Bubur Kacang Hijau Rendah Gula", "Kacang hijau rebus lembut dengan santan encer dan pemanis alami sedikit gula kelapa.", "dalam_rumah", "lansia", 15));
        rows.add(new Rekomendasi(9, "workout", "Latihan Mobilitas Sendi Lansia", "Gerakan mengangkat lengan perlahan, memutar pergelangan kaki, dan mengayun kaki bertumpu pada kursi.", "semua", "lansia", 15));
        rows.add(new Rekomendasi(10, "workout", "Workout HIIT Ringan Tanpa Lompat (Low Impact)", "Variasi gerakan squat perlahan, march in place, dan wall push-up untuk membakar kalori tanpa beban sendi.", "dalam_rumah", "dewasa", 20));
        rows.add(new Rekomendasi(11, "workout", "Peregangan Leher & Tulang Belakang di Kantor", "Gerakan chin tuck, shoulder roll, dan spinal twist lembut di kursi kantor.", "luar_rumah", "dewasa", 10));
        rows.add(new Rekomendasi(12, "workout", "Jalan Kaki Interval 30 Menit", "Kombinasi 3 menit jalan sedang dan 2 menit jalan cepat secara bergantian sebanyak 6 putaran.", "luar_rumah", "dewasa", 30));
        return ReadResult.demo(rows.stream()
            .filter(r -> matches(type, r.getTipe(), false))
            .filter(r -> matches(context, r.getGolonganKesibukan(), true))
            .filter(r -> matches(age, r.getGolonganUsia(), true))
            .toList());
    }
    private static boolean matches(String requested, String actual, boolean acceptsGeneral) {
        return requested == null || requested.isEmpty() || "semua".equalsIgnoreCase(requested)
            || requested.equalsIgnoreCase(actual) || acceptsGeneral && "semua".equalsIgnoreCase(actual);
    }
}
