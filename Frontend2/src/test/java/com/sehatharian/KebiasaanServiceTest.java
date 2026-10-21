package com.sehatharian;

import com.sehatharian.service.KebiasaanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Validasi Pelacak Kebiasaan (KebiasaanService)")
public class KebiasaanServiceTest {

    private KebiasaanService service;

    @BeforeEach
    void setUp() {
        service = new KebiasaanService();
    }

    @Test
    @DisplayName("Validasi batas hidrasi: kurang dari 10 ml atau lebih dari 5000 ml harus ditolak")
    void testValidasiBatasHidrasi() {
        LocalDate today = LocalDate.now();

        // Di bawah 10 ml harus throw IllegalArgumentException
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            service.tambahHidrasi(1, today, 5, "Air sedikit");
        });
        assertTrue(ex1.getMessage().contains("antara 10 ml sampai 5000 ml"));

        // 0 ml harus ditolak
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahHidrasi(1, today, 0, "Nol ml");
        });

        // Di atas 5000 ml harus ditolak
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahHidrasi(1, today, 5001, "Galon berlebihan");
        });
    }

    @Test
    @DisplayName("Validasi batas aktivitas: durasi non-positif atau melebihi 720 menit harus ditolak")
    void testValidasiBatasAktivitas() {
        LocalDate today = LocalDate.now();

        // Jenis kosong
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahAktivitas(1, today, "   ", 30, "Latihan");
        });

        // Durasi 0 menit
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahAktivitas(1, today, "Jalan Santai", 0, "Latihan");
        });

        // Durasi negatif
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahAktivitas(1, today, "Jalan Santai", -15, "Latihan");
        });

        // Durasi melebihi 12 jam (720 menit)
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahAktivitas(1, today, "Jalan Santai", 721, "Latihan");
        });
    }

    @Test
    @DisplayName("Validasi menu makan: nama menu dan waktu makan wajib diisi")
    void testValidasiBatasMakan() {
        LocalDate today = LocalDate.now();

        // Menu kosong
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahMakan(1, today, "sarapan", "   ", "Catatan");
        });

        // Waktu makan kosong
        assertThrows(IllegalArgumentException.class, () -> {
            service.tambahMakan(1, today, "", "Bubur Ayam Sehat", "Catatan");
        });
    }
}
