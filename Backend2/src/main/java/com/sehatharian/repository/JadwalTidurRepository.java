package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.JadwalTidur;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JadwalTidurRepository {

    public List<JadwalTidur> getRiwayatByPengguna(int penggunaId, int limit) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getRiwayatTidur(penggunaId, limit);
        }

        List<JadwalTidur> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, " +
                     "memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? " +
                     "ORDER BY tanggal DESC, id DESC" + (limit > 0 ? " LIMIT " + limit : "");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToJadwalTidur(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getRiwayatByPengguna jadwal_tidur: " + e.getMessage());
            return InMemoryDataStore.getInstance().getRiwayatTidur(penggunaId, limit);
        }
    }

    public JadwalTidur getTerbaruByPengguna(int penggunaId) throws SQLException {
        List<JadwalTidur> list = getRiwayatByPengguna(penggunaId, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    public JadwalTidur getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getJadwalTidurByTanggal(penggunaId, tanggal);
        }

        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, " +
                     "memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal = ? LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToJadwalTidur(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada getByTanggal jadwal_tidur: " + e.getMessage());
        }
        return InMemoryDataStore.getInstance().getJadwalTidurByTanggal(penggunaId, tanggal);
    }

    public int simpan(JadwalTidur jt) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().simpanJadwalTidur(jt);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO jadwal_tidur (pengguna_id, tanggal, jam_tidur, jam_bangun, " +
                     "durasi_jam, memenuhi_standar, pengingat_aktif) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE jam_tidur = VALUES(jam_tidur), " +
                     "jam_bangun = VALUES(jam_bangun), durasi_jam = VALUES(durasi_jam), " +
                     "memenuhi_standar = VALUES(memenuhi_standar), pengingat_aktif = VALUES(pengingat_aktif)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, jt.getPenggunaId());
            ps.setDate(2, Date.valueOf(jt.getTanggal()));
            ps.setTime(3, Time.valueOf(jt.getJamTidur()));
            ps.setTime(4, Time.valueOf(jt.getJamBangun()));
            ps.setDouble(5, jt.getDurasiJam());
            ps.setBoolean(6, jt.isMemenuhiStandar());
            ps.setBoolean(7, jt.isPengingatAktif());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    jt.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada simpan jadwal_tidur: " + e.getMessage());
        }
        return inMemoryId;
    }

    public boolean setPengingatAktif(int id, boolean aktif) throws SQLException {
        InMemoryDataStore.getInstance().setPengingatTidurAktif(id, aktif);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "UPDATE jadwal_tidur SET pengingat_aktif = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada setPengingatAktif: " + e.getMessage());
            return true;
        }
    }

    public boolean hapus(int id) throws SQLException {
        InMemoryDataStore.getInstance().hapusJadwalTidur(id);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "DELETE FROM jadwal_tidur WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada hapus jadwal_tidur: " + e.getMessage());
            return true;
        }
    }

    private JadwalTidur mapResultSetToJadwalTidur(ResultSet rs) throws SQLException {
        JadwalTidur jt = new JadwalTidur();
        jt.setId(rs.getInt("id"));
        jt.setPenggunaId(rs.getInt("pengguna_id"));
        jt.setTanggal(rs.getDate("tanggal").toLocalDate());
        jt.setJamTidur(rs.getTime("jam_tidur").toLocalTime());
        jt.setJamBangun(rs.getTime("jam_bangun").toLocalTime());
        jt.setDurasiJam(rs.getDouble("durasi_jam"));
        jt.setMemenuhiStandar(rs.getBoolean("memenuhi_standar"));
        jt.setPengingatAktif(rs.getBoolean("pengingat_aktif"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            jt.setCreatedAt(ts.toLocalDateTime());
        }
        return jt;
    }
}
