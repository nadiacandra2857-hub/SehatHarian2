package com.sehatharian.service;

import com.sehatharian.model.*;
import com.sehatharian.repository.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EvaluasiService {
    private final SessionAccess access;
    private final RiwayatTantanganRepository riwayatRepo;
    private final JadwalTidurRepository tidurRepo;
    private final AktivitasRepository aktivitasRepo;
    private final HidrasiRepository hidrasiRepo;
    private final EvaluasiMingguanRepository evaluasiRepo;

    public EvaluasiService(SessionAccess access) {
        this.access = java.util.Objects.requireNonNull(access);
        this.riwayatRepo = new RiwayatTantanganRepository(access);
        this.tidurRepo = new JadwalTidurRepository(access);
        this.aktivitasRepo = new AktivitasRepository(access);
        this.hidrasiRepo = new HidrasiRepository(access);
        this.evaluasiRepo = new EvaluasiMingguanRepository(access);
    }

    public EvaluasiService(SessionAccess access, RiwayatTantanganRepository riwayatRepo,
                           JadwalTidurRepository tidurRepo,
                           AktivitasRepository aktivitasRepo,
                           HidrasiRepository hidrasiRepo,
                           EvaluasiMingguanRepository evaluasiRepo) {
        this.access = java.util.Objects.requireNonNull(access);
        this.riwayatRepo = riwayatRepo;
        this.tidurRepo = tidurRepo;
        this.aktivitasRepo = aktivitasRepo;
        this.hidrasiRepo = hidrasiRepo;
        this.evaluasiRepo = evaluasiRepo;
    }

    /**
     * F-04: Menghasilkan data konsistensi 7 hari terakhir berdasarkan data riil dari database.
     */
    public List<RingkasanKonsistensi> getKonsistensi7HariTerakhir(int penggunaId, LocalDate tanggalAkhir) throws SQLException {
        access.owner(penggunaId);
        List<RingkasanKonsistensi> result = new ArrayList<>();
        LocalDate tanggalMulai = tanggalAkhir.minusDays(6);

        // Ambil data dalam rentang
        List<RiwayatTantangan> listTantangan = access.rows(access.call(() -> riwayatRepo.getByRange(penggunaId, tanggalMulai, tanggalAkhir)), RiwayatTantangan::getPenggunaId);
        List<Aktivitas> listAktivitas = access.rows(access.call(() -> aktivitasRepo.getByRange(penggunaId, tanggalMulai, tanggalAkhir)), Aktivitas::getPenggunaId);
        List<Hidrasi> listHidrasi = access.rows(access.call(() -> hidrasiRepo.getByRange(penggunaId, tanggalMulai, tanggalAkhir)), Hidrasi::getPenggunaId);
        List<JadwalTidur> listTidur = access.rows(access.call(() -> tidurRepo.getByRange(penggunaId, tanggalMulai, tanggalAkhir)), JadwalTidur::getPenggunaId);

        for (int i = 0; i < 7; i++) {
            LocalDate tgl = tanggalMulai.plusDays(i);
            String labelHari = tgl.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.of("id", "ID"));
            RingkasanKonsistensi rk = new RingkasanKonsistensi(tgl, labelHari);
            rk.setAktivitasTercatat(listAktivitas.stream().anyMatch(a -> tgl.equals(a.getTanggal())));
            rk.setHidrasiTercatat(listHidrasi.stream().anyMatch(h -> tgl.equals(h.getTanggal())));
            rk.setTidurTercatat(listTidur.stream().anyMatch(t -> tgl.equals(t.getTanggal())));

            // Tantangan
            int selesai = 0;
            int total = 0;
            for (RiwayatTantangan rt : listTantangan) {
                if (rt.getTanggal().equals(tgl)) {
                    total++;
                    if ("selesai".equalsIgnoreCase(rt.getStatus())) {
                        selesai++;
                    }
                }
            }
            rk.setTantanganSelesai(selesai);
            rk.setTotalTantangan(total);

            // Aktivitas
            int durasiAkt = 0;
            for (Aktivitas a : listAktivitas) {
                if (a.getTanggal().equals(tgl)) {
                    durasiAkt += a.getDurasiMenit();
                }
            }
            rk.setDurasiAktivitasMenit(durasiAkt);

            // Hidrasi
            int totalAir = 0;
            for (Hidrasi h : listHidrasi) {
                if (h.getTanggal().equals(tgl)) {
                    totalAir += h.getJumlahMl();
                }
            }
            rk.setTotalHidrasiMl(totalAir);

            // Tidur
            double durasiTidur = 0.0;
            for (JadwalTidur jt : listTidur) {
                if (jt.getTanggal().equals(tgl)) {
                    durasiTidur = jt.getDurasiJam();
                    break;
                }
            }
            rk.setDurasiTidurJam(durasiTidur);

            // Hitung skor komposit harian (0 - 100%)
            double skorTantangan = (total > 0) ? ((double) selesai / total) * 40.0 : (selesai > 0 ? 40.0 : 0.0);
            double skorHidrasi = Math.min((totalAir / 2000.0) * 25.0, 25.0);
            double skorAktivitas = Math.min((durasiAkt / 20.0) * 20.0, 20.0);
            double skorTidur = (durasiTidur >= 5.0) ? 15.0 : ((durasiTidur > 0) ? (durasiTidur / 5.0) * 10.0 : 0.0);

            double totalSkor = skorTantangan + skorHidrasi + skorAktivitas + skorTidur;
            rk.setSkorHarianPersen(Math.round(totalSkor * 10.0) / 10.0);

            result.add(rk);
        }

        return access.result(result);
    }

    /**
     * F-05: Melakukan evaluasi mingguan komprehensif berdasarkan 7 hari terakhir
     * dan menyimpannya ke MySQL.
     */
    public EvaluasiMingguan buatEvaluasiMingguan(int penggunaId, LocalDate tanggalAcuan) throws SQLException {
        access.owner(penggunaId);
        List<RingkasanKonsistensi> harian = getKonsistensi7HariTerakhir(penggunaId, tanggalAcuan);
        if (harian.stream().noneMatch(RingkasanKonsistensi::hasRecordedData))
            throw new IllegalStateException("Belum ada catatan untuk dievaluasi pada periode ini.");
        LocalDate mingguMulai = tanggalAcuan.minusDays(6);

        double totalSkor = 0;
        double totalJamTidur = 0;
        int hariTidurTercatat = 0;
        int hariKurangTidur = 0;
        int totalAktivitasMenit = 0;
        int totalAirMl = 0;
        int totalTantanganSelesai = 0;
        int totalTantanganDiberikan = 0;

        for (RingkasanKonsistensi rk : harian) {
            totalSkor += rk.getSkorHarianPersen();
            if (rk.getDurasiTidurJam() > 0) {
                totalJamTidur += rk.getDurasiTidurJam();
                hariTidurTercatat++;
                if (rk.getDurasiTidurJam() < 5.0) {
                    hariKurangTidur++;
                }
            }
            totalAktivitasMenit += rk.getDurasiAktivitasMenit();
            totalAirMl += rk.getTotalHidrasiMl();
            totalTantanganSelesai += rk.getTantanganSelesai();
            totalTantanganDiberikan += rk.getTotalTantangan();
        }

        double rataRataSkor = Math.round((totalSkor / 7.0) * 10.0) / 10.0;
        double rataRataTidur = hariTidurTercatat > 0
            ? Math.round((totalJamTidur / hariTidurTercatat) * 10.0) / 10.0
            : 0.0;

        boolean memenuhiStandarTidur = (rataRataTidur >= 5.0 && hariKurangTidur == 0);

        // Susun ringkasan berbasis data nyata
        StringBuilder ringkasan = new StringBuilder();
        ringkasan.append(String.format("Periode evaluasi: %s s.d. %s. ", mingguMulai, tanggalAcuan));
        ringkasan.append(String.format("Skor konsistensi mingguan Anda mencapai %.1f%%. ", rataRataSkor));
        ringkasan.append(String.format("Menyelesaikan %d dari %d tantangan harian. ", totalTantanganSelesai, totalTantanganDiberikan));
        ringkasan.append(String.format("Total aktivitas fisik tercatat %d menit dan konsumsi air %d ml. ", totalAktivitasMenit, totalAirMl));

        if (hariTidurTercatat > 0) {
            ringkasan.append(String.format("Rata-rata durasi tidur harian adalah %.1f jam. ", rataRataTidur));
            if (!memenuhiStandarTidur) {
                ringkasan.append(String.format("PERHATIAN: Terdapat %d hari dengan waktu tidur di bawah standar minimal 5 jam. ", hariKurangTidur));
            }
        } else {
            ringkasan.append("Catatan tidur belum diisi secara berkala pada minggu ini. ");
        }

        // Susun rekomendasi solutif
        StringBuilder rekomendasi = new StringBuilder();
        if (hariTidurTercatat == 0) {
            rekomendasi.append("• Belum ada catatan tidur; durasi tidur belum dapat dinilai.\n");
        } else if (!memenuhiStandarTidur) {
            rekomendasi.append("• Prioritas Istirahat: Durasi tidur Anda kurang dari 5 jam. Sangat disarankan tidur lebih awal 30–45 menit malam ini, redupkan lampu, dan hindari paparan layar gawai minimal 30 menit sebelum tidur.\n");
        } else if (rataRataTidur >= 7.0) {
            rekomendasi.append("• Kualitas Istirahat: Pola tidur Anda sangat baik dan memenuhi standar pemulihan tubuh optimal.\n");
        } else {
            rekomendasi.append("• Optimasi Tidur: Durasi tidur mencukupi batas minimal, usahakan menambah durasi istirahat menuju 7 jam untuk pemulihan energi penuh.\n");
        }

        if (harian.stream().noneMatch(RingkasanKonsistensi::isHidrasiTercatat)) {
            rekomendasi.append("• Belum ada catatan hidrasi; jumlah yang dikonsumsi belum dapat dinilai.\n");
        } else if (totalAirMl < 10000) {
            rekomendasi.append("• Peningkatan Hidrasi: Asupan air masih di bawah anjuran. Siapkan botol air 1 liter di meja aktivitas dan minum bertahap sepanjang hari.\n");
        } else {
            rekomendasi.append("• Kebiasaan Hidrasi: Bagus! Pertahankan konsistensi konsumsi cairan tubuh agar sirkulasi dan metabolisme tetap prima.\n");
        }

        if (harian.stream().noneMatch(RingkasanKonsistensi::isAktivitasTercatat)) {
            rekomendasi.append("• Belum ada catatan aktivitas; kegiatan aktual belum dapat dinilai.\n");
        } else if (totalAktivitasMenit < 90) {
            rekomendasi.append("• Gerak Aktif: Tingkatkan aktivitas fisik harian dengan jalan kaki 15 menit atau peregangan ringan di sela waktu luang.\n");
        } else {
            rekomendasi.append("• Konsistensi Gerak: Aktivitas fisik Anda sudah aktif dan seimbang dengan ritme harian Anda.");
        }

        EvaluasiMingguan em = new EvaluasiMingguan(0, access.owner(), mingguMulai, rataRataSkor,
            hariTidurTercatat > 0 ? rataRataTidur : null, memenuhiStandarTidur, ringkasan.toString(), rekomendasi.toString());

        int id = access.call(() -> evaluasiRepo.simpan(em));
        if (id <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
        em.setId(id);
        return access.result(em);
    }

    public EvaluasiMingguan getEvaluasiTerbaru(int penggunaId) throws SQLException {
        access.owner(penggunaId);
        return access.row(access.call(() -> evaluasiRepo.getTerbaruByPengguna(penggunaId)), EvaluasiMingguan::getPenggunaId);
    }

    public List<EvaluasiMingguan> getDaftarEvaluasi(int penggunaId) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> evaluasiRepo.getAllByPengguna(penggunaId)), EvaluasiMingguan::getPenggunaId);
    }
}
