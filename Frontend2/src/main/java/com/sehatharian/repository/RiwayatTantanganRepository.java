package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.RiwayatTantangan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RiwayatTantanganRepository {

    public List<RiwayatTantangan> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT rt.id, rt.pengguna_id, rt.tantangan_id, rt.tanggal, rt.durasi_target_menit, " +
                     "rt.status, rt.diselesaikan_pada, rt.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan rt " +
                     "JOIN tantangan t ON rt.tantangan_id = t.id " +
                     "WHERE rt.pengguna_id = ? AND rt.tanggal = ? " +
                     "ORDER BY rt.id ASC";
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

    public List<RiwayatTantangan> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT rt.id, rt.pengguna_id, rt.tantangan_id, rt.tanggal, rt.durasi_target_menit, " +
                     "rt.status, rt.diselesaikan_pada, rt.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan rt " +
                     "JOIN tantangan t ON rt.tantangan_id = t.id " +
                     "WHERE rt.pengguna_id = ? AND rt.tanggal BETWEEN ? AND ? " +
                     "ORDER BY rt.tanggal ASC, rt.id ASC";
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

    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE riwayat_tantangan SET status = ?, diselesaikan_pada = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if ("selesai".equalsIgnoreCase(status)) {
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int tambah(RiwayatTantangan rt) throws SQLException {
        String sql = "INSERT INTO riwayat_tantangan (pengguna_id, tantangan_id, tanggal, durasi_target_menit, status, diselesaikan_pada) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rt.getPenggunaId());
            ps.setInt(2, rt.getTantanganId());
            ps.setDate(3, Date.valueOf(rt.getTanggal()));
            ps.setInt(4, rt.getDurasiTargetMenit());
            ps.setString(5, rt.getStatus());
            if (rt.getDiselesaikanPada() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(rt.getDiselesaikanPada()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean existsForPenggunaAndTantanganOnDate(int penggunaId, int tantanganId, LocalDate tanggal) throws SQLException {
        String sql = "SELECT COUNT(*) FROM riwayat_tantangan WHERE pengguna_id = ? AND tantangan_id = ? AND tanggal = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setInt(2, tantanganId);
            ps.setDate(3, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private RiwayatTantangan mapResultSet(ResultSet rs) throws SQLException {
        RiwayatTantangan rt = new RiwayatTantangan();
        rt.setId(rs.getInt("id"));
        rt.setPenggunaId(rs.getInt("pengguna_id"));
        rt.setTantanganId(rs.getInt("tantangan_id"));
        rt.setTanggal(rs.getDate("tanggal").toLocalDate());
        rt.setDurasiTargetMenit(rs.getInt("durasi_target_menit"));
        rt.setStatus(rs.getString("status"));
        Timestamp selesai = rs.getTimestamp("diselesaikan_pada");
        if (selesai != null) {
            rt.setDiselesaikanPada(selesai.toLocalDateTime());
        }
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            rt.setCreatedAt(created.toLocalDateTime());
        }
        rt.setTantanganJudul(rs.getString("tantangan_judul"));
        rt.setTantanganDeskripsi(rs.getString("tantangan_deskripsi"));
        rt.setTantanganKategori(rs.getString("tantangan_kategori"));
        return rt;
    }
}
