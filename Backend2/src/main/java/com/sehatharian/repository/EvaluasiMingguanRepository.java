package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.EvaluasiMingguan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluasiMingguanRepository {

    public List<EvaluasiMingguan> getAllByPengguna(int penggunaId) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getAllEvaluasi(penggunaId);
        }

        List<EvaluasiMingguan> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, " +
                     "memenuhi_standar_tidur, ringkasan, rekomendasi, dibuat_pada " +
                     "FROM evaluasi_mingguan WHERE pengguna_id = ? ORDER BY minggu_mulai DESC, id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEvaluasi(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getAllByPengguna evaluasi_mingguan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getAllEvaluasi(penggunaId);
        }
    }

    public EvaluasiMingguan getTerbaruByPengguna(int penggunaId) throws SQLException {
        List<EvaluasiMingguan> list = getAllByPengguna(penggunaId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int simpan(EvaluasiMingguan em) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().simpanEvaluasi(em);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO evaluasi_mingguan (pengguna_id, minggu_mulai, skor_konsistensi, " +
                     "durasi_tidur_rata_rata, memenuhi_standar_tidur, ringkasan, rekomendasi) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE skor_konsistensi = VALUES(skor_konsistensi), " +
                     "durasi_tidur_rata_rata = VALUES(durasi_tidur_rata_rata), " +
                     "memenuhi_standar_tidur = VALUES(memenuhi_standar_tidur), " +
                     "ringkasan = VALUES(ringkasan), rekomendasi = VALUES(rekomendasi)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, em.getPenggunaId());
            ps.setDate(2, Date.valueOf(em.getMingguMulai()));
            ps.setDouble(3, em.getSkorKonsistensi());

            if (em.getDurasiTidurRataRata() != null) {
                ps.setDouble(4, em.getDurasiTidurRataRata());
            } else {
                ps.setNull(4, Types.DECIMAL);
            }

            ps.setBoolean(5, em.isMemenuhiStandarTidur());
            ps.setString(6, em.getRingkasan());
            ps.setString(7, em.getRekomendasi());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    em.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada simpan evaluasi_mingguan: " + e.getMessage());
        }
        return inMemoryId;
    }

    private EvaluasiMingguan mapResultSetToEvaluasi(ResultSet rs) throws SQLException {
        EvaluasiMingguan em = new EvaluasiMingguan();
        em.setId(rs.getInt("id"));
        em.setPenggunaId(rs.getInt("pengguna_id"));
        em.setMingguMulai(rs.getDate("minggu_mulai").toLocalDate());
        em.setSkorKonsistensi(rs.getDouble("skor_konsistensi"));

        double tidur = rs.getDouble("durasi_tidur_rata_rata");
        if (!rs.wasNull()) {
            em.setDurasiTidurRataRata(tidur);
        } else {
            em.setDurasiTidurRataRata(null);
        }

        em.setMemenuhiStandarTidur(rs.getBoolean("memenuhi_standar_tidur"));
        em.setRingkasan(rs.getString("ringkasan"));
        em.setRekomendasi(rs.getString("rekomendasi"));

        Timestamp ts = rs.getTimestamp("dibuat_pada");
        if (ts != null) {
            em.setDibuatPada(ts.toLocalDateTime());
        }
        return em;
    }
}
