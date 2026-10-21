package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.EvaluasiMingguan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluasiMingguanRepository {

    public EvaluasiMingguan getTerbaruByPengguna(int penggunaId) throws SQLException {
        String sql = "SELECT id, pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, " +
                     "memenuhi_standar_tidur, ringkasan, rekomendasi, dibuat_pada " +
                     "FROM evaluasi_mingguan WHERE pengguna_id = ? ORDER BY minggu_mulai DESC, id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<EvaluasiMingguan> getAllByPengguna(int penggunaId) throws SQLException {
        List<EvaluasiMingguan> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, " +
                     "memenuhi_standar_tidur, ringkasan, rekomendasi, dibuat_pada " +
                     "FROM evaluasi_mingguan WHERE pengguna_id = ? ORDER BY minggu_mulai DESC, id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public int simpan(EvaluasiMingguan em) throws SQLException {
        String sql = "INSERT INTO evaluasi_mingguan (pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, " +
                     "memenuhi_standar_tidur, ringkasan, rekomendasi) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, em.getPenggunaId());
            ps.setDate(2, Date.valueOf(em.getMingguMulai()));
            ps.setDouble(3, em.getSkorKonsistensi());
            ps.setDouble(4, em.getDurasiTidurRataRata());
            ps.setBoolean(5, em.isMemenuhiStandarTidur());
            ps.setString(6, em.getRingkasan());
            ps.setString(7, em.getRekomendasi());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private EvaluasiMingguan mapResultSet(ResultSet rs) throws SQLException {
        EvaluasiMingguan em = new EvaluasiMingguan();
        em.setId(rs.getInt("id"));
        em.setPenggunaId(rs.getInt("pengguna_id"));
        em.setMingguMulai(rs.getDate("minggu_mulai").toLocalDate());
        em.setSkorKonsistensi(rs.getDouble("skor_konsistensi"));
        em.setDurasiTidurRataRata(rs.getDouble("durasi_tidur_rata_rata"));
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
