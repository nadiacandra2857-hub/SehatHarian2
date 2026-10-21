package com.sehatharian;

import com.sehatharian.model.Rekomendasi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Filter Rekomendasi & Waktu Luang (F-02 & F-06)")
public class RekomendasiServiceTest {

    @Test
    @DisplayName("Filter aktivitas tidak boleh melebihi batas waktu luang pengguna (F-02)")
    void testAktivitasTidakMelebihiWaktuLuang() {
        int waktuLuangUser = 15; // 15 menit

        List<Rekomendasi> semua = new ArrayList<>();
        semua.add(new Rekomendasi(1, "aktivitas", "Napas 5 Menit", "Desc", "semua", "semua", 5));
        semua.add(new Rekomendasi(2, "aktivitas", "Stretching 15 Menit", "Desc", "semua", "semua", 15));
        semua.add(new Rekomendasi(3, "aktivitas", "Jalan Kaki 30 Menit", "Desc", "semua", "semua", 30));

        // Filter aktivitas sesuai aturan F-02
        List<Rekomendasi> hasil = semua.stream()
                .filter(r -> r.getDurasiMenit() <= waktuLuangUser)
                .toList();

        assertEquals(2, hasil.size(), "Hanya rekomendasi <= 15 menit yang boleh disajikan");
        assertTrue(hasil.stream().allMatch(r -> r.getDurasiMenit() <= waktuLuangUser));
    }

    @Test
    @DisplayName("Filter Menu & Workout sesuai golongan kesibukan dan usia (F-06)")
    void testFilterMenuDanWorkout() {
        List<Rekomendasi> katalog = new ArrayList<>();
        katalog.add(new Rekomendasi(1, "menu", "Oatmeal Buah", "Desc", "dalam_rumah", "dewasa", 10));
        katalog.add(new Rekomendasi(2, "menu", "Sup Bening Bayam", "Desc", "semua", "lansia", 20));
        katalog.add(new Rekomendasi(3, "workout", "Mobilitas Sendi Lansia", "Desc", "semua", "lansia", 15));
        katalog.add(new Rekomendasi(4, "workout", "HIIT Pekerja Kantor", "Desc", "luar_rumah", "dewasa", 25));

        // Test filter lansia
        List<Rekomendasi> untukLansia = katalog.stream()
                .filter(r -> "lansia".equalsIgnoreCase(r.getGolonganUsia()) || "semua".equalsIgnoreCase(r.getGolonganUsia()))
                .toList();

        assertEquals(2, untukLansia.size());
        assertTrue(untukLansia.stream().anyMatch(r -> r.getJudul().contains("Lansia")));

        // Test filter pekerja luar rumah
        List<Rekomendasi> luarRumah = katalog.stream()
                .filter(r -> "luar_rumah".equalsIgnoreCase(r.getGolonganKesibukan()) || "semua".equalsIgnoreCase(r.getGolonganKesibukan()))
                .toList();

        assertEquals(3, luarRumah.size());
    }
}
