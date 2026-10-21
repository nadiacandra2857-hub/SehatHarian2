package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Hidrasi;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HidrasiRepository {

    public List<Hidrasi> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Hidrasi> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal ASC, id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public int simpan(Hidrasi h) throws SQLException {
        String sql = "INSERT INTO hidrasi (pengguna_id, tanggal, jumlah_ml, catatan) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, h.getPenggunaId());
            ps.setDate(2, Date.valueOf(h.getTanggal()));
            ps.setInt(3, h.getJumlahMl());
            ps.setString(4, h.getCatatan());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Hidrasi h) throws SQLException {
        String sql = "UPDATE hidrasi SET tanggal = ?, jumlah_ml = ?, catatan = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(h.getTanggal()));
            ps.setInt(2, h.getJumlahMl());
            ps.setString(3, h.getCatatan());
            ps.setInt(4, h.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hapus(int id) throws SQLException {
        String sql = "DELETE FROM hidrasi WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalMlHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        String sql = "SELECT COALESCE(SUM(jumlah_ml), 0) FROM hidrasi WHERE pengguna_id = ? AND tanggal = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Hidrasi mapResultSet(ResultSet rs) throws SQLException {
        Hidrasi h = new Hidrasi();
        h.setId(rs.getInt("id"));
        h.setPenggunaId(rs.getInt("pengguna_id"));
        h.setTanggal(rs.getDate("tanggal").toLocalDate());
        h.setJumlahMl(rs.getInt("jumlah_ml"));
        h.setCatatan(rs.getString("catatan"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            h.setCreatedAt(ts.toLocalDateTime());
        }
        return h;
    }
}
