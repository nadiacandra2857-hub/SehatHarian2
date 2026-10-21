package com.sehatharian;

import com.sehatharian.service.TidurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Aturan Jam Tidur (F-07)")
public class TidurServiceTest {

    private TidurService tidurService;

    @BeforeEach
    void setUp() {
        tidurService = new TidurService();
    }

    @Test
    @DisplayName("Durasi tidur lintas tengah malam (22:30 ke 06:00 = 7.5 jam)")
    void testHitungDurasiTidurLintasTengahMalam() {
        LocalTime tidur = LocalTime.of(22, 30);
        LocalTime bangun = LocalTime.of(6, 0);

        double durasi = tidurService.hitungDurasiTidur(tidur, bangun);
        assertEquals(7.5, durasi, 0.01, "Durasi dari 22:30 sampai 06:00 harus 7.5 jam");
    }

    @Test
    @DisplayName("Durasi tidur setelah tengah malam (01:00 ke 07:30 = 6.5 jam)")
    void testHitungDurasiTidurSetelahTengahMalam() {
        LocalTime tidur = LocalTime.of(1, 0);
        LocalTime bangun = LocalTime.of(7, 30);

        double durasi = tidurService.hitungDurasiTidur(tidur, bangun);
        assertEquals(6.5, durasi, 0.01, "Durasi dari 01:00 sampai 07:30 harus 6.5 jam");
    }

    @Test
    @DisplayName("Standar tidur PRD minimal 5 jam: 5.5 jam memenuhi standar")
    void testMemenuhiStandarTidurSukses() {
        assertTrue(tidurService.apakahMemenuhiStandar(5.5), "5.5 jam harus memenuhi standar");
        assertTrue(tidurService.apakahMemenuhiStandar(5.0), "5.0 jam harus memenuhi standar");
    }

    @Test
    @DisplayName("Standar tidur PRD minimal 5 jam: 4.5 jam tidak memenuhi standar")
    void testTidakMemenuhiStandarTidur() {
        assertFalse(tidurService.apakahMemenuhiStandar(4.5), "4.5 jam tidak memenuhi standar");
        assertFalse(tidurService.apakahMemenuhiStandar(3.0), "3.0 jam tidak memenuhi standar");
    }

    @Test
    @DisplayName("Validasi batas waktu ekstrem")
    void testValidasiInputTidur() {
        LocalTime sama = LocalTime.of(22, 0);
        double durasi = tidurService.hitungDurasiTidur(sama, sama);
        // Durasi 0 atau 24 jam tergantung interpretasi, tapi tidak boleh crash
        assertTrue(durasi >= 0);
    }
}
