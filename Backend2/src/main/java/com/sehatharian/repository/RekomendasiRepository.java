package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Rekomendasi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RekomendasiRepository {

    public List<Rekomendasi> getAll() throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getAllRekomendasi();
        }

        List<Rekomendasi> list = new ArrayList<>();
        String sql = "SELECT id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit, created_at " +
                     "FROM rekomendasi ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRekomendasi(rs));
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getAll rekomendasi: " + e.getMessage());
            return InMemoryDataStore.getInstance().getAllRekomendasi();
        }
    }

    public List<Rekomendasi> getFiltered(String tipe, String kesibukan, String usia, int maxDurasi) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getFilteredRekomendasi(tipe, kesibukan, usia, maxDurasi);
        }

        List<Rekomendasi> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit, created_at " +
            "FROM rekomendasi WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (tipe != null && !tipe.isEmpty() && !"semua".equalsIgnoreCase(tipe)) {
            sql.append(" AND tipe = ?");
            params.add(tipe);
        }

        if (kesibukan != null && !kesibukan.isEmpty() && !"semua".equalsIgnoreCase(kesibukan)) {
            sql.append(" AND (golongan_kesibukan = ? OR golongan_kesibukan = 'semua')");
            params.add(kesibukan);
        }

        if (usia != null && !usia.isEmpty() && !"semua".equalsIgnoreCase(usia)) {
            sql.append(" AND (golongan_usia = ? OR golongan_usia = 'semua')");
            params.add(usia);
        }

        if (maxDurasi > 0) {
            sql.append(" AND durasi_menit <= ?");
            params.add(maxDurasi);
        }

        sql.append(" ORDER BY id ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRekomendasi(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getFiltered rekomendasi: " + e.getMessage());
            return InMemoryDataStore.getInstance().getFilteredRekomendasi(tipe, kesibukan, usia, maxDurasi);
        }
    }

    private Rekomendasi mapResultSetToRekomendasi(ResultSet rs) throws SQLException {
        Rekomendasi r = new Rekomendasi();
        r.setId(rs.getInt("id"));
        r.setTipe(rs.getString("tipe"));
        r.setJudul(rs.getString("judul"));
        r.setDeskripsi(rs.getString("deskripsi"));
        r.setGolonganKesibukan(rs.getString("golongan_kesibukan"));
        r.setGolonganUsia(rs.getString("golongan_usia"));
        r.setDurasiMenit(rs.getInt("durasi_menit"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            r.setCreatedAt(ts.toLocalDateTime());
        }
        return r;
    }
}
