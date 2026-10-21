package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Pengguna;

import java.sql.*;

public class PenggunaRepository {

    public Pengguna getDefaultPengguna() throws SQLException {
        String sql = "SELECT id, nama, tanggal_lahir, golongan_usia, golongan_kesibukan, waktu_luang_menit, created_at " +
                     "FROM pengguna ORDER BY id ASC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToPengguna(rs);
            }
        }
        return null;
    }

    public Pengguna getById(int id) throws SQLException {
        String sql = "SELECT id, nama, tanggal_lahir, golongan_usia, golongan_kesibukan, waktu_luang_menit, created_at " +
                     "FROM pengguna WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPengguna(rs);
                }
            }
        }
        return null;
    }

    public boolean updatePengguna(Pengguna p) throws SQLException {
        String sql = "UPDATE pengguna SET nama = ?, tanggal_lahir = ?, golongan_usia = ?, " +
                     "golongan_kesibukan = ?, waktu_luang_menit = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNama());
            if (p.getTanggalLahir() != null) {
                ps.setDate(2, Date.valueOf(p.getTanggalLahir()));
            } else {
                ps.setNull(2, Types.DATE);
            }
            ps.setString(3, p.getGolonganUsia());
            ps.setString(4, p.getGolonganKesibukan());
            ps.setInt(5, p.getWaktuLuangMenit());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public int createPengguna(Pengguna p) throws SQLException {
        String sql = "INSERT INTO pengguna (nama, tanggal_lahir, golongan_usia, golongan_kesibukan, waktu_luang_menit) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNama());
            if (p.getTanggalLahir() != null) {
                ps.setDate(2, Date.valueOf(p.getTanggalLahir()));
            } else {
                ps.setNull(2, Types.DATE);
            }
            ps.setString(3, p.getGolonganUsia());
            ps.setString(4, p.getGolonganKesibukan());
            ps.setInt(5, p.getWaktuLuangMenit());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private Pengguna mapResultSetToPengguna(ResultSet rs) throws SQLException {
        Pengguna p = new Pengguna();
        p.setId(rs.getInt("id"));
        p.setNama(rs.getString("nama"));
        Date tgl = rs.getDate("tanggal_lahir");
        if (tgl != null) {
            p.setTanggalLahir(tgl.toLocalDate());
        }
        p.setGolonganUsia(rs.getString("golongan_usia"));
        p.setGolonganKesibukan(rs.getString("golongan_kesibukan"));
        p.setWaktuLuangMenit(rs.getInt("waktu_luang_menit"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toLocalDateTime());
        }
        return p;
    }
}
