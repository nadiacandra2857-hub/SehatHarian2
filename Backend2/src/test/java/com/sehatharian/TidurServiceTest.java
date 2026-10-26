package com.sehatharian;

import com.sehatharian.model.JadwalTidur;
import com.sehatharian.service.TidurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class TidurServiceTest {

    private TidurService tidurService;

    @BeforeEach
    void setUp() {
        tidurService = new TidurService();
    }

    @Test
    @DisplayName("Kalkulasi durasi tidur normal pada hari yang sama")
    void testHitungDurasiNormal() {
        LocalTime tidur = LocalTime.of(13, 0);
        LocalTime bangun = LocalTime.of(15, 30);
        double durasi = tidurService.hitungDurasiTidur(tidur, bangun);
        assertEquals(2.5, durasi, 0.01);
    }

    @Test
    @DisplayName("Kalkulasi durasi tidur melintasi tengah malam (cross-midnight)")
    void testHitungDurasiCrossMidnight() {
        // Contoh 1: 22:00 ke 06:00 -> 8 jam
        double durasi1 = tidurService.hitungDurasiTidur(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertEquals(8.0, durasi1, 0.01);

        // Contoh 2: 23:30 ke 06:00 -> 6.5 jam
        double durasi2 = tidurService.hitungDurasiTidur(LocalTime.of(23, 30), LocalTime.of(6, 0));
        assertEquals(6.5, durasi2, 0.01);

        // Contoh 3: 00:15 ke 06:45 -> 6.5 jam
        double durasi3 = tidurService.hitungDurasiTidur(LocalTime.of(0, 15), LocalTime.of(6, 45));
        assertEquals(6.5, durasi3, 0.01);
    }

    @Test
    @DisplayName("Standar tidur minimal PRD (>= 5 jam memenuhi standar, < 5 jam memicu evaluasi)")
    void testStandarMinimalTidur5Jam() {
        // Tepat 5.0 jam -> memenuhi standar
        assertTrue(tidurService.apakahMemenuhiStandar(5.0));

        // Lebih dari 5 jam -> memenuhi standar
        assertTrue(tidurService.apakahMemenuhiStandar(7.5));

        // 4 jam 59 menit (~4.98 jam) -> TIDAK memenuhi standar (< 5 jam)
        assertFalse(tidurService.apakahMemenuhiStandar(4.98));

        // 3.5 jam -> TIDAK memenuhi standar
        assertFalse(tidurService.apakahMemenuhiStandar(3.5));
    }

    @Test
    @DisplayName("Pencatatan jam tidur memvalidasi rentang durasi wajar")
    void testCatatJamTidurValidation() {
        LocalDate today = LocalDate.now();

        // 1. Waktu null
        assertThrows(IllegalArgumentException.class, () ->
            tidurService.catatJamTidur(1, today, null, LocalTime.of(6, 0), true)
        );

        // 2. Durasi kurang dari 30 menit (misal tidur 06:00 bangun 06:15)
        assertThrows(IllegalArgumentException.class, () ->
            tidurService.catatJamTidur(1, today, LocalTime.of(6, 0), LocalTime.of(6, 15), true)
        );

        // 3. Durasi tidak wajar (misal tidur 21 jam)
        assertThrows(IllegalArgumentException.class, () ->
            tidurService.catatJamTidur(1, today, LocalTime.of(6, 0), LocalTime.of(5, 0), true)
        );
    }

    @Test
    @DisplayName("Pencatatan jam tidur berhasil menyimpan data ke repository")
    void testCatatJamTidurSuccess() throws SQLException {
        LocalDate today = LocalDate.now();
        JadwalTidur jt = tidurService.catatJamTidur(
            1, today, LocalTime.of(23, 0), LocalTime.of(6, 0), true
        );

        assertNotNull(jt);
        assertTrue(jt.getId() > 0);
        assertEquals(7.0, jt.getDurasiJam(), 0.01);
        assertTrue(jt.isMemenuhiStandar());
    }
}
