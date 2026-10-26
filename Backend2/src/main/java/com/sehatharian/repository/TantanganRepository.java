package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Tantangan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TantanganRepository {

    public List<Tantangan> getAll() throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getAllTantangan();
        }

        List<Tantangan> list = new ArrayList<>();
        String sql = "SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia, created_at " +
                     "FROM tantangan ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTantangan(rs));
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getAll tantangan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getAllTantangan();
        }
    }

    public Tantangan getById(int id) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getTantanganById(id);
        }

        String sql = "SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia, created_at " +
                     "FROM tantangan WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTantangan(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada getById tantangan: " + e.getMessage());
        }
        return InMemoryDataStore.getInstance().getTantanganById(id);
    }

    public List<Tantangan> getByFilter(String kesibukan, String usia, int maxDurasi) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getTantanganByFilter(kesibukan, usia, maxDurasi);
        }

        List<Tantangan> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia, created_at " +
            "FROM tantangan WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

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
                    list.add(mapResultSetToTantangan(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByFilter tantangan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getTantanganByFilter(kesibukan, usia, maxDurasi);
        }
    }

    private Tantangan mapResultSetToTantangan(ResultSet rs) throws SQLException {
        Tantangan t = new Tantangan();
        t.setId(rs.getInt("id"));
        t.setJudul(rs.getString("judul"));
        t.setDeskripsi(rs.getString("deskripsi"));
        t.setKategori(rs.getString("kategori"));
        t.setDurasiMenit(rs.getInt("durasi_menit"));
        t.setGolonganKesibukan(rs.getString("golongan_kesibukan"));
        t.setGolonganUsia(rs.getString("golongan_usia"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            t.setCreatedAt(ts.toLocalDateTime());
        }
        return t;
    }
}
