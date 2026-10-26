package com.sehatharian;

import com.sehatharian.model.Rekomendasi;
import com.sehatharian.service.RekomendasiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RekomendasiServiceTest {

    private RekomendasiService service;

    @BeforeEach
    void setUp() {
        service = new RekomendasiService();
    }

    @Test
    @DisplayName("Rekomendasi aktivitas membatasi durasi sesuai waktu luang pengguna (F-02)")
    void testAktivitasSesuaiWaktuLuang() throws SQLException {
        int maxWaktuLuang = 15;
        List<Rekomendasi> list = service.getAktivitasSesuaiWaktuLuang(maxWaktuLuang, "luar_rumah", "dewasa");

        assertNotNull(list);
        assertFalse(list.isEmpty());
        for (Rekomendasi r : list) {
            assertEquals("aktivitas", r.getTipe());
            assertTrue(r.getDurasiMenit() <= maxWaktuLuang,
                "Durasi rekomendasi (" + r.getDurasiMenit() + ") tidak boleh melebihi waktu luang (" + maxWaktuLuang + ")");
        }
    }

    @Test
    @DisplayName("Filter katalog menu dan workout sesuai kesibukan dan usia (F-06)")
    void testFilterMenuDanWorkout() throws SQLException {
        // 1. Menu Sehat Lansia
        List<Rekomendasi> menuLansia = service.getRekomendasiMenuDanWorkout("menu", "semua", "lansia");
        assertNotNull(menuLansia);
        assertFalse(menuLansia.isEmpty());
        for (Rekomendasi r : menuLansia) {
            assertEquals("menu", r.getTipe());
            assertTrue("lansia".equalsIgnoreCase(r.getGolonganUsia()) || "semua".equalsIgnoreCase(r.getGolonganUsia()));
        }

        // 2. Workout Dewasa di Dalam Rumah
        List<Rekomendasi> workoutIndoor = service.getRekomendasiMenuDanWorkout("workout", "dalam_rumah", "dewasa");
        assertNotNull(workoutIndoor);
        assertFalse(workoutIndoor.isEmpty());
        for (Rekomendasi r : workoutIndoor) {
            assertEquals("workout", r.getTipe());
            assertTrue("dalam_rumah".equalsIgnoreCase(r.getGolonganKesibukan()) || "semua".equalsIgnoreCase(r.getGolonganKesibukan()));
        }
    }
}
