package com.sehatharian;

import com.sehatharian.model.ProgresHarian;
import com.sehatharian.service.ProgresHarianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ProgresHarianServiceTest {

    private ProgresHarianService service;

    @BeforeEach
    void setUp() {
        service = new ProgresHarianService();
    }

    @Test
    @DisplayName("Kalkulasi persentase menangani pembagian dengan nol secara aman")
    void testHitungPersentaseDivisionByZeroSafe() {
        // Target 0
        assertEquals(0.0, service.hitungPersentasePencapaian(0, 0));
        assertEquals(100.0, service.hitungPersentasePencapaian(10, 0));

        // Normal
        assertEquals(50.0, service.hitungPersentasePencapaian(1000, 2000));
        assertEquals(100.0, service.hitungPersentasePencapaian(2000, 2000));
        assertEquals(100.0, service.hitungPersentasePencapaian(2500, 2000), "Dibatasi maksimal 100%");
    }

    @Test
    @DisplayName("Kalkulasi skor komposit 4 pilar hidup sehat")
    void testHitungSkorKomposit() {
        // Kasus 1: Semua tercapai 100% dan tidur 7 jam (>= 5 jam)
        // 40% (tantangan) + 25% (hidrasi) + 20% (aktivitas) + 15% (tidur) = 100%
        double skor1 = service.hitungSkorKomposit(100.0, 100.0, 100.0, 7.0);
        assertEquals(100.0, skor1, 0.1);

        // Kasus 2: Semua 0
        double skor2 = service.hitungSkorKomposit(0.0, 0.0, 0.0, 0.0);
        assertEquals(0.0, skor2, 0.1);

        // Kasus 3: Tantangan 50%, hidrasi 80%, aktivitas 50%, tidur 4 jam (< 5 jam)
        // (50*0.4) + (80*0.25) + (50*0.2) + (4/5*10) = 20 + 20 + 10 + 8 = 58%
        double skor3 = service.hitungSkorKomposit(50.0, 80.0, 50.0, 4.0);
        assertEquals(58.0, skor3, 0.1);
    }

    @Test
    @DisplayName("Pengambilan progres hari ini menghasilkan data teragregasi")
    void testGetProgresHari() throws SQLException {
        LocalDate today = LocalDate.now();
        ProgresHarian ph = service.getProgresHari(1, today);

        assertNotNull(ph);
        assertEquals(1, ph.getPenggunaId());
        assertEquals(today, ph.getTanggal());
        assertTrue(ph.getPersenHidrasi() >= 0.0);
        assertTrue(ph.getSkorKompositHarian() >= 0.0 && ph.getSkorKompositHarian() <= 100.0);
    }
}
