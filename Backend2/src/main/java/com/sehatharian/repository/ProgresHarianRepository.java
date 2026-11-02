package com.sehatharian.repository;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.ProgresHarian;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * ProgresHarianRepository menjalankan query agregasi metrik harian
 * dari tabel riwayat_tantangan, hidrasi, aktivitas, dan jadwal_tidur.
 */
public class ProgresHarianRepository extends OwnedRepository {
    public ProgresHarianRepository(SessionAccess access) { super(access); }
    public ProgresHarianRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) { super(access, connections); }

    public ProgresHarian getProgresHari(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        ProgresHarian p = new ProgresHarian();
        p.setPenggunaId(penggunaId);
        p.setTanggal(tanggal);

        {
            try (Connection conn = open()) {
                // 1. Agregasi Tantangan
                String sqlTantangan = "SELECT " +
                        "COUNT(*) AS total, " +
                        "SUM(CASE WHEN status = 'selesai' THEN 1 ELSE 0 END) AS selesai " +
                        "FROM riwayat_tantangan WHERE pengguna_id = ? AND tanggal = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlTantangan)) {
                    ps.setInt(1, penggunaId);
                    ps.setDate(2, Date.valueOf(tanggal));
                    try (ResultSet rs = access.call(ps::executeQuery)) {
                        if (rs.next()) {
                            int total = rs.getInt("total");
                            int selesai = rs.getInt("selesai");
                            p.setTotalTantangan(total);
                            p.setTantanganSelesai(selesai);
                            p.setPersenTantangan(total > 0 ? ((double) selesai / total) * 100.0 : 0.0);
                        }
                    }
                }

                // 2. Agregasi Hidrasi
                String sqlHidrasi = "SELECT COALESCE(SUM(jumlah_ml), 0) AS total_ml FROM hidrasi WHERE pengguna_id = ? AND tanggal = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlHidrasi)) {
                    ps.setInt(1, penggunaId);
                    ps.setDate(2, Date.valueOf(tanggal));
                    try (ResultSet rs = access.call(ps::executeQuery)) {
                        if (rs.next()) {
                            int totalMl = rs.getInt("total_ml");
                            p.setTotalHidrasiMl(totalMl);
                            p.setPersenHidrasi(Math.min(((double) totalMl / p.getTargetHidrasiMl()) * 100.0, 100.0));
                        }
                    }
                }

                // 3. Agregasi Aktivitas
                String sqlAktivitas = "SELECT COALESCE(SUM(durasi_menit), 0) AS total_menit FROM aktivitas WHERE pengguna_id = ? AND tanggal = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlAktivitas)) {
                    ps.setInt(1, penggunaId);
                    ps.setDate(2, Date.valueOf(tanggal));
                    try (ResultSet rs = access.call(ps::executeQuery)) {
                        if (rs.next()) {
                            int totalMenit = rs.getInt("total_menit");
                            p.setTotalAktivitasMenit(totalMenit);
                            p.setPersenAktivitas(Math.min(((double) totalMenit / p.getTargetAktivitasMenit()) * 100.0, 100.0));
                        }
                    }
                }

                // 4. Tidur
                String sqlTidur = "SELECT durasi_jam, memenuhi_standar FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sqlTidur)) {
                    ps.setInt(1, penggunaId);
                    ps.setDate(2, Date.valueOf(tanggal));
                    try (ResultSet rs = access.call(ps::executeQuery)) {
                        if (rs.next()) {
                            p.setDurasiTidurJam(rs.getDouble("durasi_jam"));
                            p.setTidurMemenuhiStandar(rs.getBoolean("memenuhi_standar"));
                        } else {
                            p.setDurasiTidurJam(0.0);
                            p.setTidurMemenuhiStandar(false);
                        }
                    }
                }

                // Hitung skor komposit harian
                double bobotTantangan = p.getPersenTantangan() * 0.40;
                double bobotHidrasi = p.getPersenHidrasi() * 0.25;
                double bobotAktivitas = p.getPersenAktivitas() * 0.20;
                double bobotTidur = (p.getDurasiTidurJam() >= 5.0 ? 15.0 : (p.getDurasiTidurJam() > 0 ? (p.getDurasiTidurJam() / 5.0) * 10.0 : 0.0));
                p.setSkorKompositHarian(Math.round((bobotTantangan + bobotHidrasi + bobotAktivitas + bobotTidur) * 10.0) / 10.0);

                return access.result(p);
            }
        }
    }
}
