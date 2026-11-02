package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Rekomendasi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** SQL catalog only. An empty result or SQL failure never selects sample data. */
public class RekomendasiRepository {
    private final PenggunaRepository.ConnectionFactory connections;

    public RekomendasiRepository() { this(DatabaseConfig::getConnection); }
    public RekomendasiRepository(PenggunaRepository.ConnectionFactory connections) {
        this.connections = java.util.Objects.requireNonNull(connections);
    }

    public List<Rekomendasi> getAll() throws SQLException {
        List<Rekomendasi> list = new ArrayList<>();
        String sql = "SELECT id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit, created_at " +
                     "FROM rekomendasi ORDER BY id ASC";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRekomendasi(rs));
            }
            return list;
        }
    }

    public List<Rekomendasi> getFiltered(String tipe, String kesibukan, String usia, int maxDurasi) throws SQLException {
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

        try (Connection conn = connections.open();
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
