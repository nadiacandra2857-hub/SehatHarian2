package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Rekomendasi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RekomendasiRepository {

    public List<Rekomendasi> getAll() throws SQLException {
        List<Rekomendasi> list = new ArrayList<>();
        String sql = "SELECT id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit FROM rekomendasi ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Rekomendasi> getFiltered(String tipe, String kesibukan, String usia, int maxDurasi) throws SQLException {
        List<Rekomendasi> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit FROM rekomendasi WHERE 1=1 ");

        if (tipe != null && !tipe.isEmpty() && !"semua".equalsIgnoreCase(tipe)) {
            sql.append(" AND tipe = ?");
        }
        if (kesibukan != null && !kesibukan.isEmpty() && !"semua".equalsIgnoreCase(kesibukan)) {
            sql.append(" AND (golongan_kesibukan = ? OR golongan_kesibukan = 'semua')");
        }
        if (usia != null && !usia.isEmpty() && !"semua".equalsIgnoreCase(usia)) {
            sql.append(" AND (golongan_usia = ? OR golongan_usia = 'semua')");
        }
        if (maxDurasi > 0) {
            sql.append(" AND durasi_menit <= ?");
        }
        sql.append(" ORDER BY durasi_menit ASC, id ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (tipe != null && !tipe.isEmpty() && !"semua".equalsIgnoreCase(tipe)) {
                ps.setString(idx++, tipe);
            }
            if (kesibukan != null && !kesibukan.isEmpty() && !"semua".equalsIgnoreCase(kesibukan)) {
                ps.setString(idx++, kesibukan);
            }
            if (usia != null && !usia.isEmpty() && !"semua".equalsIgnoreCase(usia)) {
                ps.setString(idx++, usia);
            }
            if (maxDurasi > 0) {
                ps.setInt(idx++, maxDurasi);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    private Rekomendasi mapResultSet(ResultSet rs) throws SQLException {
        return new Rekomendasi(
            rs.getInt("id"),
            rs.getString("tipe"),
            rs.getString("judul"),
            rs.getString("deskripsi"),
            rs.getString("golongan_kesibukan"),
            rs.getString("golongan_usia"),
            rs.getInt("durasi_menit")
        );
    }
}
