package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.JadwalTidur;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JadwalTidurRepository {

    public List<JadwalTidur> getRiwayatByPengguna(int penggunaId, int limit) throws SQLException {
        List<JadwalTidur> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? ORDER BY tanggal DESC, id DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setInt(2, limit > 0 ? limit : 30);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public JadwalTidur getTerbaruByPengguna(int penggunaId) throws SQLException {
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? ORDER BY tanggal DESC, id DESC LIMIT 1";
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

    public JadwalTidur getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public int simpan(JadwalTidur j) throws SQLException {
        String sql = "INSERT INTO jadwal_tidur (pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, j.getPenggunaId());
            ps.setDate(2, Date.valueOf(j.getTanggal()));
            ps.setTime(3, Time.valueOf(j.getJamTidur()));
            ps.setTime(4, Time.valueOf(j.getJamBangun()));
            ps.setDouble(5, j.getDurasiJam());
            ps.setBoolean(6, j.isMemenuhiStandar());
            ps.setBoolean(7, j.isPengingatAktif());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(JadwalTidur j) throws SQLException {
        String sql = "UPDATE jadwal_tidur SET tanggal = ?, jam_tidur = ?, jam_bangun = ?, durasi_jam = ?, " +
                     "memenuhi_standar = ?, pengingat_aktif = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(j.getTanggal()));
            ps.setTime(2, Time.valueOf(j.getJamTidur()));
            ps.setTime(3, Time.valueOf(j.getJamBangun()));
            ps.setDouble(4, j.getDurasiJam());
            ps.setBoolean(5, j.isMemenuhiStandar());
            ps.setBoolean(6, j.isPengingatAktif());
            ps.setInt(7, j.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setPengingatAktif(int id, boolean aktif) throws SQLException {
        String sql = "UPDATE jadwal_tidur SET pengingat_aktif = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hapus(int id) throws SQLException {
        String sql = "DELETE FROM jadwal_tidur WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private JadwalTidur mapResultSet(ResultSet rs) throws SQLException {
        JadwalTidur j = new JadwalTidur();
        j.setId(rs.getInt("id"));
        j.setPenggunaId(rs.getInt("pengguna_id"));
        j.setTanggal(rs.getDate("tanggal").toLocalDate());
        j.setJamTidur(rs.getTime("jam_tidur").toLocalTime());
        j.setJamBangun(rs.getTime("jam_bangun").toLocalTime());
        j.setDurasiJam(rs.getDouble("durasi_jam"));
        j.setMemenuhiStandar(rs.getBoolean("memenuhi_standar"));
        j.setPengingatAktif(rs.getBoolean("pengingat_aktif"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            j.setCreatedAt(ts.toLocalDateTime());
        }
        return j;
    }
}
