package com.sehatharian;

import com.sehatharian.model.EvaluasiMingguan;
import com.sehatharian.model.RingkasanKonsistensi;
import com.sehatharian.service.EvaluasiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluasiServiceTest {

    private EvaluasiService evaluasiService;

    @BeforeEach
    void setUp() {
        evaluasiService = new EvaluasiService();
    }

    @Test
    @DisplayName("Konsistensi 7 hari terakhir menghasilkan tepat 7 record harian")
    void testKonsistensi7HariTerakhir() throws SQLException {
        LocalDate today = LocalDate.now();
        List<RingkasanKonsistensi> list = evaluasiService.getKonsistensi7HariTerakhir(1, today);

        assertNotNull(list);
        assertEquals(7, list.size(), "Harus ada tepat 7 record harian untuk 1 minggu");

        // Record terakhir harus bertanggal hari ini
        assertEquals(today, list.get(6).getTanggal());
        assertEquals(today.minusDays(6), list.get(0).getTanggal());
    }

    @Test
    @DisplayName("Pembuatan evaluasi mingguan menghasilkan skor dan rekomendasi tersimpan")
    void testBuatEvaluasiMingguan() throws SQLException {
        LocalDate today = LocalDate.now();
        EvaluasiMingguan em = evaluasiService.buatEvaluasiMingguan(1, today);

        assertNotNull(em);
        assertTrue(em.getId() > 0, "ID evaluasi harus dihasilkan");
        assertEquals(1, em.getPenggunaId());
        assertTrue(em.getSkorKonsistensi() >= 0.0 && em.getSkorKonsistensi() <= 100.0);
        assertNotNull(em.getRingkasan());
        assertNotNull(em.getRekomendasi());
        assertNotNull(em.getGradeKonsistensi());
    }

    @Test
    @DisplayName("Penanganan riwayat evaluasi yang tersimpan")
    void testGetDaftarEvaluasi() throws SQLException {
        List<EvaluasiMingguan> daftar = evaluasiService.getDaftarEvaluasi(1);
        assertNotNull(daftar);
    }
}
