package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Hidrasi;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HidrasiRepository {

    public List<Hidrasi> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getHidrasiByTanggal(penggunaId, tanggal);
        }

        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHidrasi(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByTanggal hidrasi: " + e.getMessage());
            return InMemoryDataStore.getInstance().getHidrasiByTanggal(penggunaId, tanggal);
        }
    }

    public List<Hidrasi> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getHidrasiByRange(penggunaId, start, end);
        }

        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal ASC, id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHidrasi(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByRange hidrasi: " + e.getMessage());
            return InMemoryDataStore.getInstance().getHidrasiByRange(penggunaId, start, end);
        }
    }

    public int simpan(Hidrasi h) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().tambahHidrasi(h);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO hidrasi (pengguna_id, tanggal, jumlah_ml, catatan) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, h.getPenggunaId());
            ps.setDate(2, Date.valueOf(h.getTanggal()));
            ps.setInt(3, h.getJumlahMl());
            ps.setString(4, h.getCatatan());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    h.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada simpan hidrasi: " + e.getMessage());
        }
        return inMemoryId;
    }

    public boolean hapus(int id) throws SQLException {
        InMemoryDataStore.getInstance().hapusHidrasi(id);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "DELETE FROM hidrasi WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada hapus hidrasi: " + e.getMessage());
            return true;
        }
    }

    public int getTotalMlHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getTotalHidrasiHariIni(penggunaId, tanggal);
        }

        String sql = "SELECT COALESCE(SUM(jumlah_ml), 0) FROM hidrasi WHERE pengguna_id = ? AND tanggal = ?";
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
            System.err.println("Database error pada getTotalMlHariIni hidrasi: " + e.getMessage());
        }
        return InMemoryDataStore.getInstance().getTotalHidrasiHariIni(penggunaId, tanggal);
    }

    private Hidrasi mapResultSetToHidrasi(ResultSet rs) throws SQLException {
        Hidrasi h = new Hidrasi();
        h.setId(rs.getInt("id"));
        h.setPenggunaId(rs.getInt("pengguna_id"));
        h.setTanggal(rs.getDate("tanggal").toLocalDate());
        h.setJumlahMl(rs.getInt("jumlah_ml"));
        h.setCatatan(rs.getString("catatan"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            h.setCreatedAt(ts.toLocalDateTime());
        }
        return h;
    }
}
