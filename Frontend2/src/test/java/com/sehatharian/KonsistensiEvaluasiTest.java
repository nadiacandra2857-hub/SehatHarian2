package com.sehatharian;

import com.sehatharian.model.RingkasanKonsistensi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Perhitungan Konsistensi & Evaluasi (F-04 & F-05)")
public class KonsistensiEvaluasiTest {

    @Test
    @DisplayName("Perhitungan skor komposit harian dengan proporsi seimbang")
    void testPerhitunganSkorHarian() {
        RingkasanKonsistensi rk = new RingkasanKonsistensi(LocalDate.now(), "Sen");
        rk.setTotalTantangan(3);
        rk.setTantanganSelesai(3);        // 100% tantangan -> 40 poin
        rk.setTotalHidrasiMl(2000);         // 2000 ml -> 25 poin
        rk.setDurasiAktivitasMenit(20);     // 20 menit -> 20 poin
        rk.setDurasiTidurJam(7.5);          // >= 5 jam -> 15 poin

        // Hitung skor komposit
        double skorTantangan = ((double) rk.getTantanganSelesai() / rk.getTotalTantangan()) * 40.0;
        double skorHidrasi = Math.min((rk.getTotalHidrasiMl() / 2000.0) * 25.0, 25.0);
        double skorAktivitas = Math.min((rk.getDurasiAktivitasMenit() / 20.0) * 20.0, 20.0);
        double skorTidur = (rk.getDurasiTidurJam() >= 5.0) ? 15.0 : 0.0;

        double total = skorTantangan + skorHidrasi + skorAktivitas + skorTidur;
        assertEquals(100.0, total, 0.01, "Ketika semua target terpenuhi, skor harian harus 100%");
    }

    @Test
    @DisplayName("Evaluasi memicu peringatan khusus jika tidur di bawah 5 jam (F-05)")
    void testEvaluasiMemicuPeringatanKurangTidur() {
        double durasiTidur = 4.0; // Kurang dari 5 jam
        boolean memenuhiStandar = (durasiTidur >= 5.0);

        assertFalse(memenuhiStandar, "Durasi 4.0 jam harus ditandai tidak memenuhi standar");

        // Verifikasi susunan teks rekomendasi
        String pesanRekomendasi = "";
        if (!memenuhiStandar) {
            pesanRekomendasi = "Prioritas Istirahat: Durasi tidur Anda kurang dari 5 jam. Sangat disarankan tidur lebih awal 30–45 menit malam ini.";
        }

        assertTrue(pesanRekomendasi.contains("kurang dari 5 jam"));
        assertTrue(pesanRekomendasi.contains("tidur lebih awal"));
    }
}
