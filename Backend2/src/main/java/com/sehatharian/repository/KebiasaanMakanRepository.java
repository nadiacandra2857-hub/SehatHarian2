package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.KebiasaanMakan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KebiasaanMakanRepository {

    public List<KebiasaanMakan> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getMakanByTanggal(penggunaId, tanggal);
        }

        List<KebiasaanMakan> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, waktu_makan, menu, keterangan, created_at " +
                     "FROM kebiasaan_makan WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMakan(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByTanggal kebiasaan_makan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getMakanByTanggal(penggunaId, tanggal);
        }
    }

    public int simpan(KebiasaanMakan km) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().tambahMakan(km);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO kebiasaan_makan (pengguna_id, tanggal, waktu_makan, menu, keterangan) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, km.getPenggunaId());
            ps.setDate(2, Date.valueOf(km.getTanggal()));
            ps.setString(3, km.getWaktuMakan());
            ps.setString(4, km.getMenu());
            ps.setString(5, km.getKeterangan());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    km.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada simpan kebiasaan_makan: " + e.getMessage());
        }
        return inMemoryId;
    }

    public boolean hapus(int id) throws SQLException {
        InMemoryDataStore.getInstance().hapusMakan(id);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "DELETE FROM kebiasaan_makan WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada hapus kebiasaan_makan: " + e.getMessage());
            return true;
        }
    }

    private KebiasaanMakan mapResultSetToMakan(ResultSet rs) throws SQLException {
        KebiasaanMakan km = new KebiasaanMakan();
        km.setId(rs.getInt("id"));
        km.setPenggunaId(rs.getInt("pengguna_id"));
        km.setTanggal(rs.getDate("tanggal").toLocalDate());
        km.setWaktuMakan(rs.getString("waktu_makan"));
        km.setMenu(rs.getString("menu"));
        km.setKeterangan(rs.getString("keterangan"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            km.setCreatedAt(ts.toLocalDateTime());
        }
        return km;
    }
}
