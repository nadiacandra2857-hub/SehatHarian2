package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Aktivitas;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AktivitasRepository {

    public List<Aktivitas> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getAktivitasByTanggal(penggunaId, tanggal);
        }

        List<Aktivitas> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan, created_at " +
                     "FROM aktivitas WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAktivitas(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByTanggal aktivitas: " + e.getMessage());
            return InMemoryDataStore.getInstance().getAktivitasByTanggal(penggunaId, tanggal);
        }
    }

    public List<Aktivitas> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getAktivitasByRange(penggunaId, start, end);
        }

        List<Aktivitas> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan, created_at " +
                     "FROM aktivitas WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal ASC, id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAktivitas(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByRange aktivitas: " + e.getMessage());
            return InMemoryDataStore.getInstance().getAktivitasByRange(penggunaId, start, end);
        }
    }

    public int simpan(Aktivitas a) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().tambahAktivitas(a);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO aktivitas (pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getPenggunaId());
            ps.setDate(2, Date.valueOf(a.getTanggal()));
            ps.setString(3, a.getJenisAktivitas());
            ps.setInt(4, a.getDurasiMenit());
            ps.setString(5, a.getKeterangan());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    a.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada simpan aktivitas: " + e.getMessage());
        }
        return inMemoryId;
    }

    public boolean hapus(int id) throws SQLException {
        InMemoryDataStore.getInstance().hapusAktivitas(id);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "DELETE FROM aktivitas WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada hapus aktivitas: " + e.getMessage());
            return true;
        }
    }

    public int getTotalDurasiHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getTotalAktivitasHariIni(penggunaId, tanggal);
        }

        String sql = "SELECT COALESCE(SUM(durasi_menit), 0) FROM aktivitas WHERE pengguna_id = ? AND tanggal = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada getTotalDurasiHariIni aktivitas: " + e.getMessage());
        }
        return InMemoryDataStore.getInstance().getTotalAktivitasHariIni(penggunaId, tanggal);
    }

    private Aktivitas mapResultSetToAktivitas(ResultSet rs) throws SQLException {
        Aktivitas a = new Aktivitas();
        a.setId(rs.getInt("id"));
        a.setPenggunaId(rs.getInt("pengguna_id"));
        a.setTanggal(rs.getDate("tanggal").toLocalDate());
        a.setJenisAktivitas(rs.getString("jenis_aktivitas"));
        a.setDurasiMenit(rs.getInt("durasi_menit"));
        a.setKeterangan(rs.getString("keterangan"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}
