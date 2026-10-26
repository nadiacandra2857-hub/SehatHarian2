package com.sehatharian;

import com.sehatharian.model.*;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pengujian Integrasi Seluruh Fitur Frontend (Tantangan, Rekomendasi, Kebiasaan, Tidur, Evaluasi, Profil)")
class FrontendFeatureFlowIntegrationTest {

    private TantanganService tantanganService;
    private RekomendasiService rekomendasiService;
    private KebiasaanService kebiasaanService;
    private TidurService tidurService;
    private EvaluasiService evaluasiService;
    private PenggunaRepository penggunaRepo;

    @BeforeEach
    void setUp() {
        tantanganService = new TantanganService();
        rekomendasiService = new RekomendasiService();
        kebiasaanService = new KebiasaanService();
        tidurService = new TidurService();
        evaluasiService = new EvaluasiService();
        penggunaRepo = new PenggunaRepository();
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Fitur F-01 & F-09: Tantangan harian dapat dimuat dan diselesaikan")
    void testTantanganFlow() throws SQLException {
        Pengguna p = penggunaRepo.getDefaultPengguna();
        assertNotNull(p);

        LocalDate today = LocalDate.now();
        List<RiwayatTantangan> list = tantanganService.pastikanTantanganHariIni(p, today);
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Tantangan harian harus tersedia");

        // Ambil salah satu tantangan dan selesaikan
        RiwayatTantangan target = list.get(0);
        boolean success = tantanganService.selesaikanTantangan(target.getId());
        assertTrue(success, "Tantangan harus berhasil ditandai selesai");

        // Cek kembali statusnya
        List<RiwayatTantangan> updatedList = tantanganService.getTantanganByTanggal(p.getId(), today);
        assertTrue(updatedList.stream().anyMatch(rt -> rt.getId() == target.getId() && "selesai".equalsIgnoreCase(rt.getStatus())));
    }

    @Test
    @DisplayName("Fitur F-02 & F-06: Rekomendasi aktivitas, menu, dan workout tersaring dengan benar")
    void testRekomendasiFlow() throws SQLException {
        // Rekomendasi sesuai waktu luang (misal: 15 menit)
        List<Rekomendasi> aktivitas15Menit = rekomendasiService.getAktivitasSesuaiWaktuLuang(15, "semua", "dewasa");
        assertNotNull(aktivitas15Menit);
        assertTrue(aktivitas15Menit.stream().allMatch(r -> r.getDurasiMenit() <= 15), "Durasi aktivitas tidak boleh melebihi waktu luang");

        // Filter menu lansia
        List<Rekomendasi> menuLansia = rekomendasiService.getRekomendasiMenuDanWorkout("menu", "semua", "lansia");
        assertNotNull(menuLansia);
        assertFalse(menuLansia.isEmpty());
        assertTrue(menuLansia.stream().allMatch(r -> "menu".equalsIgnoreCase(r.getTipe())));
    }

    @Test
    @DisplayName("Fitur F-03: Pencatatan kebiasaan hidrasi, aktivitas, dan makan")
    void testKebiasaanFlow() throws SQLException {
        Pengguna p = penggunaRepo.getDefaultPengguna();
        LocalDate today = LocalDate.now();

        // 1. Hidrasi
        int initialHidrasi = kebiasaanService.getTotalHidrasiMl(p.getId(), today);
        kebiasaanService.tambahHidrasi(p.getId(), today, 300, "Air minum sore");
        int updatedHidrasi = kebiasaanService.getTotalHidrasiMl(p.getId(), today);
        assertEquals(initialHidrasi + 300, updatedHidrasi);

        // 2. Aktivitas
        int initialAktivitas = kebiasaanService.getTotalDurasiAktivitas(p.getId(), today);
        kebiasaanService.tambahAktivitas(p.getId(), today, "Jogging sore", 25, "Lari di taman");
        int updatedAktivitas = kebiasaanService.getTotalDurasiAktivitas(p.getId(), today);
        assertEquals(initialAktivitas + 25, updatedAktivitas);

        // 3. Kebiasaan Makan
        kebiasaanService.tambahMakan(p.getId(), today, "makan_malam", "Ikan bakar & tumis kangkung", "Gizi seimbang");
        List<KebiasaanMakan> makanList = kebiasaanService.getMakanHariIni(p.getId(), today);
        assertTrue(makanList.stream().anyMatch(km -> km.getMenu().contains("Ikan bakar")));
    }

    @Test
    @DisplayName("Fitur F-07 & F-08: Pencatatan tidur lintas tengah malam dan standar PRD minimal 5 jam")
    void testTidurFlow() throws SQLException {
        Pengguna p = penggunaRepo.getDefaultPengguna();
        LocalDate today = LocalDate.now();

        // Catat tidur 23:00 ke 06:30 (7.5 jam -> memenuhi standar)
        JadwalTidur jt = tidurService.catatJamTidur(p.getId(), today, LocalTime.of(23, 0), LocalTime.of(6, 30), true);
        assertNotNull(jt);
        assertEquals(7.5, jt.getDurasiJam(), 0.01);
        assertTrue(jt.isMemenuhiStandar());
        assertTrue(jt.isPengingatAktif());

        // Update pengingat
        boolean updated = tidurService.updatePengingat(jt.getId(), false);
        assertTrue(updated);
    }

    @Test
    @DisplayName("Fitur F-04 & F-05: Perhitungan konsistensi 7 hari dan pembuatan evaluasi mingguan")
    void testEvaluasiDanKonsistensiFlow() throws SQLException {
        Pengguna p = penggunaRepo.getDefaultPengguna();
        LocalDate today = LocalDate.now();

        List<RingkasanKonsistensi> list7Hari = evaluasiService.getKonsistensi7HariTerakhir(p.getId(), today);
        assertEquals(7, list7Hari.size(), "Harus ada tepat 7 hari data");

        for (RingkasanKonsistensi rk : list7Hari) {
            assertTrue(rk.getSkorHarianPersen() >= 0.0 && rk.getSkorHarianPersen() <= 100.0, "Skor harian harus 0 - 100%");
        }

        // Buat evaluasi mingguan
        EvaluasiMingguan em = evaluasiService.buatEvaluasiMingguan(p.getId(), today);
        assertNotNull(em);
        assertTrue(em.getSkorKonsistensi() > 0);
        assertNotNull(em.getRingkasan());
        assertNotNull(em.getRekomendasi());
    }

    @Test
    @DisplayName("Fitur Profil: Pembaruan profil pengguna dan persistensi state")
    void testProfilFlow() throws SQLException {
        Pengguna p = penggunaRepo.getDefaultPengguna();
        p.setNama("Nadia Candra Testing");
        p.setWaktuLuangMenit(60);

        boolean updated = penggunaRepo.updatePengguna(p);
        assertTrue(updated);

        Pengguna refreshed = penggunaRepo.getDefaultPengguna();
        assertEquals("Nadia Candra Testing", refreshed.getNama());
        assertEquals(60, refreshed.getWaktuLuangMenit());
    }
}
