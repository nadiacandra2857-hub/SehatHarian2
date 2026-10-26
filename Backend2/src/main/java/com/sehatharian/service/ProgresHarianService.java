package com.sehatharian.service;

import com.sehatharian.model.ProgresHarian;
import com.sehatharian.repository.ProgresHarianRepository;

import java.sql.SQLException;
import java.time.LocalDate;

public class ProgresHarianService {

    private final ProgresHarianRepository repo;

    public ProgresHarianService() {
        this.repo = new ProgresHarianRepository();
    }

    public ProgresHarianService(ProgresHarianRepository repo) {
        this.repo = repo;
    }

    public ProgresHarian getProgresHari(int penggunaId, LocalDate tanggal) throws SQLException {
        return repo.getProgresHari(penggunaId, tanggal);
    }

    /**
     * Menghitung persentase pencapaian dengan aman (mencegah pembagian dengan nol).
     *
     * @param capaian Nilai aktual yang dicapai
     * @param target Nilai target acuan
     * @return Persentase pencapaian (0.0 sampai 100.0, dibulatkan ke 1 desimal)
     */
    public double hitungPersentasePencapaian(double capaian, double target) {
        if (target <= 0.0) {
            return capaian > 0.0 ? 100.0 : 0.0;
        }
        if (capaian <= 0.0) {
            return 0.0;
        }
        double ratio = (capaian / target) * 100.0;
        double clamped = Math.min(Math.max(ratio, 0.0), 100.0);
        return Math.round(clamped * 10.0) / 10.0;
    }

    /**
     * Menghitung skor komposit harian (0.0 - 100.0%) berdasarkan 4 pilar kebiasaan sehat:
     * - Tantangan harian: bobot 40%
     * - Hidrasi air: bobot 25%
     * - Aktivitas fisik: bobot 20%
     * - Kualitas tidur: bobot 15% (15% penuh jika >= 5 jam)
     */
    public double hitungSkorKomposit(double persenTantangan, double persenHidrasi,
                                     double persenAktivitas, double durasiTidurJam) {
        double bobotTantangan = Math.min(Math.max(persenTantangan, 0.0), 100.0) * 0.40;
        double bobotHidrasi = Math.min(Math.max(persenHidrasi, 0.0), 100.0) * 0.25;
        double bobotAktivitas = Math.min(Math.max(persenAktivitas, 0.0), 100.0) * 0.20;

        double bobotTidur = 0.0;
        if (durasiTidurJam >= 5.0) {
            bobotTidur = 15.0;
        } else if (durasiTidurJam > 0.0) {
            bobotTidur = (durasiTidurJam / 5.0) * 10.0;
        }

        double total = bobotTantangan + bobotHidrasi + bobotAktivitas + bobotTidur;
        return Math.round(total * 10.0) / 10.0;
    }
}
