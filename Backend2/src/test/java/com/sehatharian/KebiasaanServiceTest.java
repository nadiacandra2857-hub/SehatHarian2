package com.sehatharian;

import com.sehatharian.model.Aktivitas;
import com.sehatharian.model.Hidrasi;
import com.sehatharian.model.KebiasaanMakan;
import com.sehatharian.service.KebiasaanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KebiasaanServiceTest {

    private KebiasaanService service;

    @BeforeEach
    void setUp() {
        service = new KebiasaanService();
    }

    @Test
    @DisplayName("Pencatatan aktivitas fisik dengan validasi input batas")
    void testAktivitasFisik() throws SQLException {
        LocalDate today = LocalDate.now();

        // Valid
        service.tambahAktivitas(1, today, "Lompat Tali", 15, "Pemanasan pagi");
        List<Aktivitas> list = service.getAktivitasHariIni(1, today);
        assertFalse(list.isEmpty());

        // Invalid: jenis kosong
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahAktivitas(1, today, "", 15, "Catatan")
        );

        // Invalid: durasi 0 atau lebih dari 720
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahAktivitas(1, today, "Jalan", 0, "Catatan")
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahAktivitas(1, today, "Jalan", 800, "Catatan")
        );
    }

    @Test
    @DisplayName("Pencatatan asupan hidrasi air dengan validasi volume ml")
    void testHidrasi() throws SQLException {
        LocalDate today = LocalDate.now();

        // Valid
        service.tambahHidrasi(1, today, 300, "1 gelas air");
        List<Hidrasi> list = service.getHidrasiHariIni(1, today);
        assertFalse(list.isEmpty());

        // Invalid: kurang dari 10 ml atau lebih dari 5000 ml
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahHidrasi(1, today, 5, "Terlalu sedikit")
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahHidrasi(1, today, 6000, "Terlalu banyak")
        );
    }

    @Test
    @DisplayName("Pencatatan kebiasaan makan dengan validasi kategori waktu")
    void testKebiasaanMakan() throws SQLException {
        LocalDate today = LocalDate.now();

        // Valid
        service.tambahMakan(1, today, "sarapan", "Nasi Uduk Sayur", "Pagi");
        List<KebiasaanMakan> list = service.getMakanHariIni(1, today);
        assertFalse(list.isEmpty());

        // Invalid: menu kosong
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahMakan(1, today, "sarapan", "", "Pagi")
        );

        // Invalid: kategori waktu tidak valid
        assertThrows(IllegalArgumentException.class, () ->
            service.tambahMakan(1, today, "makan_tengah_malam", "Burger", "Malam")
        );
    }
}
