package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Tantangan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TantanganRepository {

    public List<Tantangan> getAll() throws SQLException {
        List<Tantangan> list = new ArrayList<>();
        String sql = "SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia FROM tantangan ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Tantangan getById(int id) throws SQLException {
        String sql = "SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia FROM tantangan WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Tantangan> getByFilter(String kesibukan, String usia, int maxDurasiMenit) throws SQLException {
        List<Tantangan> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia FROM tantangan WHERE 1=1 ");

        if (kesibukan != null && !kesibukan.isEmpty() && !"semua".equalsIgnoreCase(kesibukan)) {
            sql.append(" AND (golongan_kesibukan = ? OR golongan_kesibukan = 'semua')");
        }
        if (usia != null && !usia.isEmpty() && !"semua".equalsIgnoreCase(usia)) {
            sql.append(" AND (golongan_usia = ? OR golongan_usia = 'semua')");
        }
        if (maxDurasiMenit > 0) {
            sql.append(" AND durasi_menit <= ?");
        }
        sql.append(" ORDER BY durasi_menit ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (kesibukan != null && !kesibukan.isEmpty() && !"semua".equalsIgnoreCase(kesibukan)) {
                ps.setString(paramIndex++, kesibukan);
            }
            if (usia != null && !usia.isEmpty() && !"semua".equalsIgnoreCase(usia)) {
                ps.setString(paramIndex++, usia);
            }
            if (maxDurasiMenit > 0) {
                ps.setInt(paramIndex++, maxDurasiMenit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    private Tantangan mapResultSet(ResultSet rs) throws SQLException {
        return new Tantangan(
            rs.getInt("id"),
            rs.getString("judul"),
            rs.getString("deskripsi"),
            rs.getString("kategori"),
            rs.getInt("durasi_menit"),
            rs.getString("golongan_kesibukan"),
            rs.getString("golongan_usia")
        );
    }
}
