package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.RiwayatTantangan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RiwayatTantanganRepository {

    public List<RiwayatTantangan> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getRiwayatTantanganByTanggal(penggunaId, tanggal);
        }

        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT r.id, r.pengguna_id, r.tantangan_id, r.tanggal, r.durasi_target_menit, " +
                     "r.status, r.diselesaikan_pada, r.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan r " +
                     "JOIN tantangan t ON r.tantangan_id = t.id " +
                     "WHERE r.pengguna_id = ? AND r.tanggal = ? " +
                     "ORDER BY r.id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRiwayat(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByTanggal riwayat_tantangan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getRiwayatTantanganByTanggal(penggunaId, tanggal);
        }
    }

    public List<RiwayatTantangan> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().getRiwayatTantanganByRange(penggunaId, start, end);
        }

        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT r.id, r.pengguna_id, r.tantangan_id, r.tanggal, r.durasi_target_menit, " +
                     "r.status, r.diselesaikan_pada, r.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan r " +
                     "JOIN tantangan t ON r.tantangan_id = t.id " +
                     "WHERE r.pengguna_id = ? AND r.tanggal BETWEEN ? AND ? " +
                     "ORDER BY r.tanggal ASC, r.id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRiwayat(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Database error pada getByRange riwayat_tantangan: " + e.getMessage());
            return InMemoryDataStore.getInstance().getRiwayatTantanganByRange(penggunaId, start, end);
        }
    }

    public int tambah(RiwayatTantangan rt) throws SQLException {
        int inMemoryId = InMemoryDataStore.getInstance().tambahRiwayatTantangan(rt);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return inMemoryId;
        }

        String sql = "INSERT INTO riwayat_tantangan (pengguna_id, tantangan_id, tanggal, durasi_target_menit, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rt.getPenggunaId());
            ps.setInt(2, rt.getTantanganId());
            ps.setDate(3, Date.valueOf(rt.getTanggal()));
            ps.setInt(4, rt.getDurasiTargetMenit());
            ps.setString(5, rt.getStatus() != null ? rt.getStatus() : "disarankan");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    rt.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada tambah riwayat_tantangan: " + e.getMessage());
        }
        return inMemoryId;
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        InMemoryDataStore.getInstance().updateStatusRiwayatTantangan(id, status);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

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
        } catch (SQLException e) {
            System.err.println("Database error pada updateStatus riwayat_tantangan: " + e.getMessage());
            return true;
        }
    }

    public boolean existsForDate(int penggunaId, int tantanganId, LocalDate tanggal) throws SQLException {
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return InMemoryDataStore.getInstance().existsRiwayatForDate(penggunaId, tantanganId, tanggal);
        }

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
        } catch (SQLException e) {
            System.err.println("Database error pada existsForDate: " + e.getMessage());
        }
        return InMemoryDataStore.getInstance().existsRiwayatForDate(penggunaId, tantanganId, tanggal);
    }

    public boolean deleteByPenggunaAndTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        InMemoryDataStore.getInstance().hapusRiwayatByPenggunaDanTanggal(penggunaId, tanggal);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "DELETE FROM riwayat_tantangan WHERE pengguna_id = ? AND tanggal = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada deleteByPenggunaAndTanggal: " + e.getMessage());
            return true;
        }
    }

    private RiwayatTantangan mapResultSetToRiwayat(ResultSet rs) throws SQLException {
        RiwayatTantangan rt = new RiwayatTantangan();
        rt.setId(rs.getInt("id"));
        rt.setPenggunaId(rs.getInt("pengguna_id"));
        rt.setTantanganId(rs.getInt("tantangan_id"));
        rt.setTanggal(rs.getDate("tanggal").toLocalDate());
        rt.setDurasiTargetMenit(rs.getInt("durasi_target_menit"));
        rt.setStatus(rs.getString("status"));

        Timestamp tsDone = rs.getTimestamp("diselesaikan_pada");
        if (tsDone != null) {
            rt.setDiselesaikanPada(tsDone.toLocalDateTime());
        }

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            rt.setCreatedAt(ts.toLocalDateTime());
        }

        // Relasi
        rt.setTantanganJudul(rs.getString("tantangan_judul"));
        rt.setTantanganDeskripsi(rs.getString("tantangan_deskripsi"));
        rt.setTantanganKategori(rs.getString("tantangan_kategori"));

        return rt;
    }
}
