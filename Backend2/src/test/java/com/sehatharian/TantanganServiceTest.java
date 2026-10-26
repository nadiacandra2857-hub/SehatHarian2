package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.RiwayatTantangan;
import com.sehatharian.service.TantanganService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TantanganServiceTest {

    private TantanganService tantanganService;
    private Pengguna dummyPengguna;

    @BeforeEach
    void setUp() {
        tantanganService = new TantanganService();
        dummyPengguna = new Pengguna(1, "Nadia Ayu Candra", LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 45);
    }

    @Test
    @DisplayName("Memastikan tantangan hari ini tersedia untuk pengguna sesuai profil")
    void testPastikanTantanganHariIni() throws SQLException {
        LocalDate today = LocalDate.now();
        List<RiwayatTantangan> list = tantanganService.pastikanTantanganHariIni(dummyPengguna, today);

        assertNotNull(list);
        assertFalse(list.isEmpty(), "Daftar tantangan hari ini tidak boleh kosong");
        assertTrue(list.size() >= 1 && list.size() <= 4, "Tantangan harian harus berkisar 1 - 3 tantangan");

        for (RiwayatTantangan rt : list) {
            assertEquals(dummyPengguna.getId(), rt.getPenggunaId());
            assertEquals(today, rt.getTanggal());
            assertNotNull(rt.getStatus());
        }
    }

    @Test
    @DisplayName("Pembaruan status tantangan menjadi 'selesai' dan 'dilewati'")
    void testUpdateStatusTantangan() throws SQLException {
        LocalDate today = LocalDate.now();
        List<RiwayatTantangan> list = tantanganService.pastikanTantanganHariIni(dummyPengguna, today);
        assertFalse(list.isEmpty());

        RiwayatTantangan rt = list.get(0);

        // 1. Selesaikan
        boolean okSelesai = tantanganService.selesaikanTantangan(rt.getId());
        assertTrue(okSelesai);

        List<RiwayatTantangan> afterSelesai = tantanganService.getTantanganByTanggal(dummyPengguna.getId(), today);
        RiwayatTantangan updated1 = afterSelesai.stream().filter(r -> r.getId() == rt.getId()).findFirst().orElse(null);
        assertNotNull(updated1);
        assertEquals("selesai", updated1.getStatus());

        // 2. Lewati
        boolean okLewati = tantanganService.lewatiTantangan(rt.getId());
        assertTrue(okLewati);

        List<RiwayatTantangan> afterLewati = tantanganService.getTantanganByTanggal(dummyPengguna.getId(), today);
        RiwayatTantangan updated2 = afterLewati.stream().filter(r -> r.getId() == rt.getId()).findFirst().orElse(null);
        assertNotNull(updated2);
        assertEquals("dilewati", updated2.getStatus());
    }

    @Test
    @DisplayName("Pencegahan duplikasi penambahan tantangan pada tanggal yang sama")
    void testPreventDuplicateChallenge() throws SQLException {
        LocalDate futureDate = LocalDate.now().plusDays(20);
        int tantanganId = 1;

        // Bersihkan data jika ada sisa eksekusi sebelumnya
        tantanganService.hapusTantanganPadaTanggal(dummyPengguna.getId(), futureDate);

        try {
            // Tambah pertama -> Berhasil
            boolean added1 = tantanganService.tambahTantanganKeHariIni(dummyPengguna.getId(), tantanganId, futureDate);
            assertTrue(added1, "Penambahan tantangan pertama harus berhasil");

            // Tambah kedua untuk tantangan yang sama pada hari yang sama -> Ditolak / False
            boolean added2 = tantanganService.tambahTantanganKeHariIni(dummyPengguna.getId(), tantanganId, futureDate);
            assertFalse(added2, "Tantangan yang sama pada tanggal yang sama tidak boleh diduplikasi");
        } finally {
            // Bersihkan setelah tes
            tantanganService.hapusTantanganPadaTanggal(dummyPengguna.getId(), futureDate);
        }
    }
}
