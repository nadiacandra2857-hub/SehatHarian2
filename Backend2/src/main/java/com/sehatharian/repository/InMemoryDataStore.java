package com.sehatharian.repository;

import com.sehatharian.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InMemoryDataStore menyediakan state penyimpanan in-memory yang lengkap dan reaktif
 * untuk aplikasi SehatHarian. Ini memastikan seluruh fitur (Autentikasi, Onboarding,
 * Tantangan, Rekomendasi, Catatan Kebiasaan, Jadwal Tidur, Grafik Statistik, dan Evaluasi Mingguan)
 * dapat berjalan dengan lancar tanpa crash atau freeze bahkan saat MySQL server lokal offline.
 */
public class InMemoryDataStore {

    private static final InMemoryDataStore INSTANCE = new InMemoryDataStore();

    public static InMemoryDataStore getInstance() {
        return INSTANCE;
    }

    private final AtomicInteger idGenerator = new AtomicInteger(100);

    private final List<Pengguna> daftarPengguna = new CopyOnWriteArrayList<>();
    private final List<Tantangan> daftarTantangan = new CopyOnWriteArrayList<>();
    private final List<RiwayatTantangan> daftarRiwayatTantangan = new CopyOnWriteArrayList<>();
    private final List<Rekomendasi> daftarRekomendasi = new CopyOnWriteArrayList<>();
    private final List<Hidrasi> daftarHidrasi = new CopyOnWriteArrayList<>();
    private final List<Aktivitas> daftarAktivitas = new CopyOnWriteArrayList<>();
    private final List<KebiasaanMakan> daftarMakan = new CopyOnWriteArrayList<>();
    private final List<JadwalTidur> daftarTidur = new CopyOnWriteArrayList<>();
    private final List<EvaluasiMingguan> daftarEvaluasi = new CopyOnWriteArrayList<>();

    private InMemoryDataStore() {
        initDefaultData();
    }

    private void initDefaultData() {
        LocalDate today = LocalDate.now();

        // 1. Pengguna Default (Password: 'password123')
        Pengguna defaultUser = new Pengguna(
            1, "Nadia Ayu Candra", "nadia@sehatharian.local",
            "$2a$10$wG0.1d80gM63wZ4tKzO90.7nUq2D/jI485n6bH4x79mK1s6U5mX8q",
            LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 45, true
        );
        daftarPengguna.add(defaultUser);

        // 2. Katalog Tantangan (Sesuai seed.sql)
        daftarTantangan.add(new Tantangan(1, "Jalan Kaki Ringan Pagi", "Luangkan waktu untuk berjalan santai di sekitar rumah atau kantor selama 15 menit untuk memompa sirkulasi darah.", "jalan kaki", 15, "semua", "semua"));
        daftarTantangan.add(new Tantangan(2, "Peregangan Meja Kerja (Desk Stretch)", "Lakukan peregangan leher, bahu, pergelangan tangan, dan punggung bagian bawah di sela waktu kerja.", "peregangan", 10, "luar_rumah", "dewasa"));
        daftarTantangan.add(new Tantangan(3, "Workout Penguat Inti di Rumah", "Gerakan peregangan dan penguatan otot dasar: plank lembut, glute bridge, dan bird-dog tanpa alat beban.", "workout di rumah", 20, "dalam_rumah", "dewasa"));
        daftarTantangan.add(new Tantangan(4, "Senam Kebugaran dan Mobilitas Sendi", "Gerakan latihan keseimbangan, rotasi sendi pergelangan kaki, lutut, dan bahu secara perlahan.", "workout di rumah", 15, "semua", "lansia"));
        daftarTantangan.add(new Tantangan(5, "Tantangan Hidrasi 2 Liter", "Pastikan tubuh terhidrasi baik dengan minum minimal 8 gelas atau 2000 ml air putih hari ini.", "hidrasi", 5, "semua", "semua"));
        daftarTantangan.add(new Tantangan(6, "Porsi Sayur & Buah Segar", "Tambahkan satu porsi sayur hijau saat makan siang dan buah segar sebagai camilan sore.", "nutrisi", 10, "semua", "semua"));
        daftarTantangan.add(new Tantangan(7, "Jalan Kaki 30 Menit Sore", "Jalan cepat di area terbuka atau taman untuk meningkatkan kebugaran kardiovaskular.", "jalan kaki", 30, "luar_rumah", "dewasa"));
        daftarTantangan.add(new Tantangan(8, "Latihan Keseimbangan Berdiri Satu Kaki", "Latihan keseimbangan statis dibantu sandaran kursi untuk melatih koordinasi dan kekuatan panggul.", "workout di rumah", 10, "dalam_rumah", "lansia"));

        // 3. Katalog Rekomendasi (Sesuai seed.sql)
        daftarRekomendasi.add(new Rekomendasi(1, "aktivitas", "Pernapasan Diafragma 5 Menit", "Duduk tegak, tarik napas dalam 4 detik, tahan 4 detik, dan hembuskan perlahan 6 detik untuk menurunkan stres.", "semua", "semua", 5));
        daftarRekomendasi.add(new Rekomendasi(2, "aktivitas", "Jalan Santai Sela Istirahat", "Manfaatkan jeda istirahat siang untuk berjalan santai 10 menit tanpa melihat layar gawai.", "luar_rumah", "dewasa", 10));
        daftarRekomendasi.add(new Rekomendasi(3, "aktivitas", "Jalan Kaki Sore 25 Menit", "Berjalan santai mengelilingi perumahan sambil menikmati udara sore yang segar.", "dalam_rumah", "semua", 25));
        daftarRekomendasi.add(new Rekomendasi(4, "aktivitas", "Peregangan Otot Kaki & Betis", "Regangkan otot betis dan paha belakang setelah duduk lama bekerja.", "semua", "dewasa", 15));

        daftarRekomendasi.add(new Rekomendasi(5, "menu", "Sarapan Oatmeal Pisang & Madu", "Semangkuk oatmeal hangat dengan irisan pisang, taburan biji chia, dan satu sendok teh madu alami.", "dalam_rumah", "dewasa", 10));
        daftarRekomendasi.add(new Rekomendasi(6, "menu", "Sup Bening Bayam, Jagung Manis & Tempe Kukus", "Menu kaya serat, rendah lemak jenuh, dan mudah dicerna untuk menjaga kestabilan tekanan darah.", "semua", "lansia", 20));
        daftarRekomendasi.add(new Rekomendasi(7, "menu", "Kotak Bekal Sehat: Nasi Merah & Ayam Panggang", "Porsi seimbang dengan nasi merah, dada ayam panggang bumbu kunyit, serta tumis buncis wortel.", "luar_rumah", "dewasa", 25));
        daftarRekomendasi.add(new Rekomendasi(8, "menu", "Bubur Kacang Hijau Rendah Gula", "Kacang hijau rebus lembut dengan santan encer dan pemanis alami sedikit gula kelapa.", "dalam_rumah", "lansia", 15));

        daftarRekomendasi.add(new Rekomendasi(9, "workout", "Latihan Mobilitas Sendi Lansia", "Gerakan mengangkat lengan perlahan, memutar pergelangan kaki, dan mengayun kaki bertumpu pada kursi.", "semua", "lansia", 15));
        daftarRekomendasi.add(new Rekomendasi(10, "workout", "Workout HIIT Ringan Tanpa Lompat (Low Impact)", "Variasi gerakan squat perlahan, march in place, dan wall push-up untuk membakar kalori tanpa beban sendi.", "dalam_rumah", "dewasa", 20));
        daftarRekomendasi.add(new Rekomendasi(11, "workout", "Peregangan Leher & Tulang Belakang di Kantor", "Gerakan chin tuck, shoulder roll, dan spinal twist lembut di kursi kantor.", "luar_rumah", "dewasa", 10));
        daftarRekomendasi.add(new Rekomendasi(12, "workout", "Jalan Kaki Interval 30 Menit", "Kombinasi 3 menit jalan sedang dan 2 menit jalan cepat secara bergantian sebanyak 6 putaran.", "luar_rumah", "dewasa", 30));

        // 4. Riwayat Tantangan Hari Ini & 6 Hari Terakhir
        RiwayatTantangan rt1 = new RiwayatTantangan(1, 1, 1, today, 15, "selesai", LocalDateTime.now().minusHours(2));
        attachTantanganDetails(rt1, 1);
        RiwayatTantangan rt2 = new RiwayatTantangan(2, 1, 2, today, 10, "disarankan", null);
        attachTantanganDetails(rt2, 2);
        RiwayatTantangan rt3 = new RiwayatTantangan(3, 1, 5, today, 5, "disarankan", null);
        attachTantanganDetails(rt3, 5);
        daftarRiwayatTantangan.addAll(List.of(rt1, rt2, rt3));

        for (int i = 1; i <= 6; i++) {
            LocalDate d = today.minusDays(i);
            RiwayatTantangan past1 = new RiwayatTantangan(idGenerator.incrementAndGet(), 1, 1, d, 15, "selesai", d.atTime(8, 0));
            attachTantanganDetails(past1, 1);
            RiwayatTantangan past2 = new RiwayatTantangan(idGenerator.incrementAndGet(), 1, 5, d, 5, "selesai", d.atTime(18, 0));
            attachTantanganDetails(past2, 5);
            daftarRiwayatTantangan.add(past1);
            daftarRiwayatTantangan.add(past2);
        }

        // 5. Hidrasi Hari Ini & 6 Hari Terakhir
        daftarHidrasi.add(new Hidrasi(1, 1, today, 600, "Minum 2 gelas air saat bangun pagi dan sarapan"));
        daftarHidrasi.add(new Hidrasi(2, 1, today, 650, "Minum sebotol air setelah jalan santai"));
        int[] hidrasiMl = {1750, 2000, 1500, 2250, 1800, 2000};
        for (int i = 1; i <= 6; i++) {
            LocalDate d = today.minusDays(i);
            daftarHidrasi.add(new Hidrasi(idGenerator.incrementAndGet(), 1, d, hidrasiMl[i - 1], "Asupan air harian " + d));
        }

        // 6. Aktivitas Hari Ini & 6 Hari Terakhir
        daftarAktivitas.add(new Aktivitas(1, 1, today, "Jalan santai pagi", 20, "Keliling perumahan sebelum beraktivitas"));
        int[] aktivitasMenit = {25, 30, 20, 35, 15, 30};
        String[] namaAktivitas = {"Jalan cepat", "Workout inti", "Desk stretch", "Bersepeda santai", "Peregangan", "Jalan santai"};
        for (int i = 1; i <= 6; i++) {
            LocalDate d = today.minusDays(i);
            daftarAktivitas.add(new Aktivitas(idGenerator.incrementAndGet(), 1, d, namaAktivitas[i - 1], aktivitasMenit[i - 1], "Aktivitas harian"));
        }

        // 7. Kebiasaan Makan Hari Ini
        daftarMakan.add(new KebiasaanMakan(1, 1, today, "sarapan", "Oatmeal buah pisang & telur rebus", "Kaya serat dan protein"));
        daftarMakan.add(new KebiasaanMakan(2, 1, today, "makan_siang", "Nasi merah & ayam panggang", "Porsi seimbang isi piringku"));

        // 8. Jadwal Tidur
        daftarTidur.add(new JadwalTidur(1, 1, today.minusDays(1), LocalTime.of(22, 30), LocalTime.of(6, 0), 7.5, true, true));
        double[] tidurJam = {7.0, 7.5, 6.5, 8.0, 7.2, 7.5};
        for (int i = 2; i <= 7; i++) {
            LocalDate d = today.minusDays(i);
            daftarTidur.add(new JadwalTidur(idGenerator.incrementAndGet(), 1, d, LocalTime.of(23, 0), LocalTime.of(6, 0), tidurJam[i - 2], true, true));
        }

        // 9. Evaluasi Mingguan
        EvaluasiMingguan em = new EvaluasiMingguan(
            1, 1, today.minusDays(7), 85.5, 7.3, true,
            "Konsistensi kebiasaan sehat Anda minggu ini sangat baik dengan skor 85.5%. Hidrasi dan jam tidur malam tercapai konsisten di atas 7 jam.",
            "Pertahankan jadwal tidur malam teratur dan luangkan waktu tambahan untuk aktivitas kardio ringan di luar ruangan."
        );
        em.setDibuatPada(LocalDateTime.now().minusDays(1));
        daftarEvaluasi.add(em);
    }

    private void attachTantanganDetails(RiwayatTantangan rt, int tantanganId) {
        for (Tantangan t : daftarTantangan) {
            if (t.getId() == tantanganId) {
                rt.setTantanganJudul(t.getJudul());
                rt.setTantanganDeskripsi(t.getDeskripsi());
                rt.setTantanganKategori(t.getKategori());
                break;
            }
        }
    }

    // ==========================================
    // PENGGUNA & AUTENTIKASI
    // ==========================================
    public synchronized Pengguna getPengguna() {
        return daftarPengguna.isEmpty() ? null : daftarPengguna.get(0);
    }

    public synchronized Pengguna getPenggunaById(int id) {
        for (Pengguna p : daftarPengguna) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public synchronized Pengguna findPenggunaByEmail(String email) {
        if (email == null) return null;
        for (Pengguna p : daftarPengguna) {
            if (email.equalsIgnoreCase(p.getEmail())) return p;
        }
        return null;
    }

    public synchronized void updatePengguna(Pengguna p) {
        if (p != null) {
            for (int i = 0; i < daftarPengguna.size(); i++) {
                if (daftarPengguna.get(i).getId() == p.getId()) {
                    daftarPengguna.set(i, p);
                    return;
                }
            }
            daftarPengguna.add(p);
        }
    }

    public synchronized int simpanPenggunaBaru(Pengguna p) {
        int id = idGenerator.incrementAndGet();
        p.setId(id);
        p.setCreatedAt(LocalDateTime.now());
        daftarPengguna.add(p);
        return id;
    }

    // ==========================================
    // TANTANGAN & RIWAYAT TANTANGAN
    // ==========================================
    public List<Tantangan> getAllTantangan() {
        return new ArrayList<>(daftarTantangan);
    }

    public Tantangan getTantanganById(int id) {
        for (Tantangan t : daftarTantangan) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public List<Tantangan> getTantanganByFilter(String kesibukan, String usia, int maxDurasi) {
        List<Tantangan> res = new ArrayList<>();
        for (Tantangan t : daftarTantangan) {
            boolean matchKesibukan = kesibukan == null || kesibukan.isEmpty() || "semua".equalsIgnoreCase(kesibukan)
                    || "semua".equalsIgnoreCase(t.getGolonganKesibukan()) || t.getGolonganKesibukan().equalsIgnoreCase(kesibukan);
            boolean matchUsia = usia == null || usia.isEmpty() || "semua".equalsIgnoreCase(usia)
                    || "semua".equalsIgnoreCase(t.getGolonganUsia()) || t.getGolonganUsia().equalsIgnoreCase(usia);
            boolean matchDurasi = maxDurasi <= 0 || t.getDurasiMenit() <= maxDurasi;

            if (matchKesibukan && matchUsia && matchDurasi) {
                res.add(t);
            }
        }
        return res;
    }

    public List<RiwayatTantangan> getRiwayatTantanganByTanggal(int penggunaId, LocalDate tanggal) {
        List<RiwayatTantangan> res = new ArrayList<>();
        for (RiwayatTantangan rt : daftarRiwayatTantangan) {
            if (rt.getPenggunaId() == penggunaId && rt.getTanggal().equals(tanggal)) {
                if (rt.getTantanganJudul() == null) {
                    attachTantanganDetails(rt, rt.getTantanganId());
                }
                res.add(rt);
            }
        }
        return res;
    }

    public List<RiwayatTantangan> getRiwayatTantanganByRange(int penggunaId, LocalDate start, LocalDate end) {
        List<RiwayatTantangan> res = new ArrayList<>();
        for (RiwayatTantangan rt : daftarRiwayatTantangan) {
            if (rt.getPenggunaId() == penggunaId && !rt.getTanggal().isBefore(start) && !rt.getTanggal().isAfter(end)) {
                if (rt.getTantanganJudul() == null) {
                    attachTantanganDetails(rt, rt.getTantanganId());
                }
                res.add(rt);
            }
        }
        return res;
    }

    public boolean updateStatusRiwayatTantangan(int id, String status) {
        for (RiwayatTantangan rt : daftarRiwayatTantangan) {
            if (rt.getId() == id) {
                rt.setStatus(status);
                if ("selesai".equalsIgnoreCase(status)) {
                    rt.setDiselesaikanPada(LocalDateTime.now());
                } else {
                    rt.setDiselesaikanPada(null);
                }
                return true;
            }
        }
        return false;
    }

    public int tambahRiwayatTantangan(RiwayatTantangan rt) {
        int newId = idGenerator.incrementAndGet();
        rt.setId(newId);
        attachTantanganDetails(rt, rt.getTantanganId());
        daftarRiwayatTantangan.add(rt);
        return newId;
    }

    public boolean existsRiwayatForDate(int penggunaId, int tantanganId, LocalDate tanggal) {
        for (RiwayatTantangan rt : daftarRiwayatTantangan) {
            if (rt.getPenggunaId() == penggunaId && rt.getTantanganId() == tantanganId && rt.getTanggal().equals(tanggal)) {
                return true;
            }
        }
        return false;
    }

    public boolean hapusRiwayatByPenggunaDanTanggal(int penggunaId, LocalDate tanggal) {
        return daftarRiwayatTantangan.removeIf(rt -> rt.getPenggunaId() == penggunaId && rt.getTanggal().equals(tanggal));
    }

    // ==========================================
    // REKOMENDASI
    // ==========================================
    public List<Rekomendasi> getAllRekomendasi() {
        return new ArrayList<>(daftarRekomendasi);
    }

    public List<Rekomendasi> getFilteredRekomendasi(String tipe, String kesibukan, String usia, int maxDurasi) {
        List<Rekomendasi> res = new ArrayList<>();
        for (Rekomendasi r : daftarRekomendasi) {
            boolean matchTipe = tipe == null || tipe.isEmpty() || "semua".equalsIgnoreCase(tipe) || r.getTipe().equalsIgnoreCase(tipe);
            boolean matchKesibukan = kesibukan == null || kesibukan.isEmpty() || "semua".equalsIgnoreCase(kesibukan)
                    || "semua".equalsIgnoreCase(r.getGolonganKesibukan()) || r.getGolonganKesibukan().equalsIgnoreCase(kesibukan);
            boolean matchUsia = usia == null || usia.isEmpty() || "semua".equalsIgnoreCase(usia)
                    || "semua".equalsIgnoreCase(r.getGolonganUsia()) || r.getGolonganUsia().equalsIgnoreCase(usia);
            boolean matchDurasi = maxDurasi <= 0 || r.getDurasiMenit() <= maxDurasi;

            if (matchTipe && matchKesibukan && matchUsia && matchDurasi) {
                res.add(r);
            }
        }
        return res;
    }

    // ==========================================
    // HIDRASI
    // ==========================================
    public List<Hidrasi> getHidrasiByTanggal(int penggunaId, LocalDate tanggal) {
        List<Hidrasi> res = new ArrayList<>();
        for (Hidrasi h : daftarHidrasi) {
            if (h.getPenggunaId() == penggunaId && h.getTanggal().equals(tanggal)) {
                res.add(h);
            }
        }
        res.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return res;
    }

    public List<Hidrasi> getHidrasiByRange(int penggunaId, LocalDate start, LocalDate end) {
        List<Hidrasi> res = new ArrayList<>();
        for (Hidrasi h : daftarHidrasi) {
            if (h.getPenggunaId() == penggunaId && !h.getTanggal().isBefore(start) && !h.getTanggal().isAfter(end)) {
                res.add(h);
            }
        }
        return res;
    }

    public int tambahHidrasi(Hidrasi h) {
        int newId = idGenerator.incrementAndGet();
        h.setId(newId);
        h.setCreatedAt(LocalDateTime.now());
        daftarHidrasi.add(h);
        return newId;
    }

    public boolean hapusHidrasi(int id) {
        return daftarHidrasi.removeIf(h -> h.getId() == id);
    }

    public int getTotalHidrasiHariIni(int penggunaId, LocalDate tanggal) {
        int total = 0;
        for (Hidrasi h : daftarHidrasi) {
            if (h.getPenggunaId() == penggunaId && h.getTanggal().equals(tanggal)) {
                total += h.getJumlahMl();
            }
        }
        return total;
    }

    // ==========================================
    // AKTIVITAS
    // ==========================================
    public List<Aktivitas> getAktivitasByTanggal(int penggunaId, LocalDate tanggal) {
        List<Aktivitas> res = new ArrayList<>();
        for (Aktivitas a : daftarAktivitas) {
            if (a.getPenggunaId() == penggunaId && a.getTanggal().equals(tanggal)) {
                res.add(a);
            }
        }
        res.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return res;
    }

    public List<Aktivitas> getAktivitasByRange(int penggunaId, LocalDate start, LocalDate end) {
        List<Aktivitas> res = new ArrayList<>();
        for (Aktivitas a : daftarAktivitas) {
            if (a.getPenggunaId() == penggunaId && !a.getTanggal().isBefore(start) && !a.getTanggal().isAfter(end)) {
                res.add(a);
            }
        }
        return res;
    }

    public int tambahAktivitas(Aktivitas a) {
        int newId = idGenerator.incrementAndGet();
        a.setId(newId);
        a.setCreatedAt(LocalDateTime.now());
        daftarAktivitas.add(a);
        return newId;
    }

    public boolean hapusAktivitas(int id) {
        return daftarAktivitas.removeIf(a -> a.getId() == id);
    }

    public int getTotalAktivitasHariIni(int penggunaId, LocalDate tanggal) {
        int total = 0;
        for (Aktivitas a : daftarAktivitas) {
            if (a.getPenggunaId() == penggunaId && a.getTanggal().equals(tanggal)) {
                total += a.getDurasiMenit();
            }
        }
        return total;
    }

    // ==========================================
    // KEBIASAAN MAKAN
    // ==========================================
    public List<KebiasaanMakan> getMakanByTanggal(int penggunaId, LocalDate tanggal) {
        List<KebiasaanMakan> res = new ArrayList<>();
        for (KebiasaanMakan km : daftarMakan) {
            if (km.getPenggunaId() == penggunaId && km.getTanggal().equals(tanggal)) {
                res.add(km);
            }
        }
        res.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return res;
    }

    public int tambahMakan(KebiasaanMakan km) {
        int newId = idGenerator.incrementAndGet();
        km.setId(newId);
        km.setCreatedAt(LocalDateTime.now());
        daftarMakan.add(km);
        return newId;
    }

    public boolean hapusMakan(int id) {
        return daftarMakan.removeIf(m -> m.getId() == id);
    }

    // ==========================================
    // JADWAL TIDUR
    // ==========================================
    public List<JadwalTidur> getRiwayatTidur(int penggunaId, int limit) {
        List<JadwalTidur> res = new ArrayList<>();
        for (JadwalTidur jt : daftarTidur) {
            if (jt.getPenggunaId() == penggunaId) {
                res.add(jt);
            }
        }
        res.sort((a, b) -> {
            int cmp = b.getTanggal().compareTo(a.getTanggal());
            return cmp != 0 ? cmp : Integer.compare(b.getId(), a.getId());
        });
        if (limit > 0 && res.size() > limit) {
            return new ArrayList<>(res.subList(0, limit));
        }
        return res;
    }

    public JadwalTidur getJadwalTidurTerbaru(int penggunaId) {
        List<JadwalTidur> list = getRiwayatTidur(penggunaId, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    public JadwalTidur getJadwalTidurByTanggal(int penggunaId, LocalDate tanggal) {
        for (JadwalTidur jt : daftarTidur) {
            if (jt.getPenggunaId() == penggunaId && jt.getTanggal().equals(tanggal)) {
                return jt;
            }
        }
        return null;
    }

    public int simpanJadwalTidur(JadwalTidur jt) {
        int newId = idGenerator.incrementAndGet();
        jt.setId(newId);
        jt.setCreatedAt(LocalDateTime.now());
        daftarTidur.removeIf(existing -> existing.getPenggunaId() == jt.getPenggunaId() && existing.getTanggal().equals(jt.getTanggal()));
        daftarTidur.add(0, jt);
        return newId;
    }

    public boolean setPengingatTidurAktif(int id, boolean aktif) {
        for (JadwalTidur jt : daftarTidur) {
            if (jt.getId() == id) {
                jt.setPengingatAktif(aktif);
                return true;
            }
        }
        return false;
    }

    public boolean hapusJadwalTidur(int id) {
        return daftarTidur.removeIf(jt -> jt.getId() == id);
    }

    // ==========================================
    // EVALUASI MINGGUAN
    // ==========================================
    public EvaluasiMingguan getEvaluasiTerbaru(int penggunaId) {
        for (EvaluasiMingguan em : daftarEvaluasi) {
            if (em.getPenggunaId() == penggunaId) {
                return em;
            }
        }
        return null;
    }

    public List<EvaluasiMingguan> getAllEvaluasi(int penggunaId) {
        List<EvaluasiMingguan> res = new ArrayList<>();
        for (EvaluasiMingguan em : daftarEvaluasi) {
            if (em.getPenggunaId() == penggunaId) {
                res.add(em);
            }
        }
        res.sort((a, b) -> b.getMingguMulai().compareTo(a.getMingguMulai()));
        return res;
    }

    public int simpanEvaluasi(EvaluasiMingguan em) {
        int newId = idGenerator.incrementAndGet();
        em.setId(newId);
        em.setDibuatPada(LocalDateTime.now());
        daftarEvaluasi.removeIf(existing -> existing.getPenggunaId() == em.getPenggunaId() && existing.getMingguMulai().equals(em.getMingguMulai()));
        daftarEvaluasi.add(0, em);
        return newId;
    }
}
