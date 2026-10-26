package com.sehatharian.repository;

import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;

import java.sql.*;

/**
 * PenggunaRepository menangani akses data dan query JDBC untuk entitas Pengguna.
 * Mendukung autentikasi kredensial, verifikasi email unik, pembaruan profil,
 * serta penyimpanan preferensi onboarding ke MySQL.
 */
public class PenggunaRepository {

    public Pengguna getDefaultPengguna() throws SQLException {
        if (DatabaseConfig.isDatabaseAvailable()) {
            String sql = "SELECT id, nama, email, password_hash, tanggal_lahir, golongan_usia, " +
                         "golongan_kesibukan, waktu_luang_menit, onboarding_selesai, created_at " +
                         "FROM pengguna ORDER BY id ASC LIMIT 1";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPengguna(rs);
                }
            } catch (SQLException e) {
                System.err.println("Database offline pada getDefaultPengguna: " + e.getMessage());
            }
        }

        // Fallback in-memory
        UserSession session = UserSession.getInstance();
        Pengguna p = InMemoryDataStore.getInstance().getPengguna();
        if (p == null) {
            p = new Pengguna(1, "Nadia Ayu Candra", "nadia@sehatharian.local", "",
                    java.time.LocalDate.of(2001, 5, 15), "dewasa", "luar_rumah", 45, true);
            InMemoryDataStore.getInstance().updatePengguna(p);
        }
        if (session.isLoggedIn() && session.getUserName() != null && !session.getUserName().trim().isEmpty()) {
            p.setNama(session.getUserName().trim());
        }
        return p;
    }

    public Pengguna getById(int id) throws SQLException {
        if (DatabaseConfig.isDatabaseAvailable()) {
            String sql = "SELECT id, nama, email, password_hash, tanggal_lahir, golongan_usia, " +
                         "golongan_kesibukan, waktu_luang_menit, onboarding_selesai, created_at " +
                         "FROM pengguna WHERE id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPengguna(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error pada getById: " + e.getMessage());
            }
        }
        return InMemoryDataStore.getInstance().getPenggunaById(id);
    }

    public Pengguna findByEmail(String email) throws SQLException {
        if (email == null) return null;
        String trimmed = email.trim();

        if (DatabaseConfig.isDatabaseAvailable()) {
            String sql = "SELECT id, nama, email, password_hash, tanggal_lahir, golongan_usia, " +
                         "golongan_kesibukan, waktu_luang_menit, onboarding_selesai, created_at " +
                         "FROM pengguna WHERE LOWER(email) = LOWER(?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, trimmed);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPengguna(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error pada findByEmail: " + e.getMessage());
            }
        }
        return InMemoryDataStore.getInstance().findPenggunaByEmail(trimmed);
    }

    public boolean existsByEmail(String email) throws SQLException {
        if (email == null) return false;
        String trimmed = email.trim();

        if (DatabaseConfig.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM pengguna WHERE LOWER(email) = LOWER(?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, trimmed);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error pada existsByEmail: " + e.getMessage());
            }
        }
        return InMemoryDataStore.getInstance().findPenggunaByEmail(trimmed) != null;
    }

    public int createPengguna(Pengguna p) throws SQLException {
        // Simpan in-memory untuk sinkronisasi state fallback
        int inMemoryId = InMemoryDataStore.getInstance().simpanPenggunaBaru(p);

        if (!DatabaseConfig.isDatabaseAvailable()) {
            p.setId(inMemoryId);
            return inMemoryId;
        }

        String sql = "INSERT INTO pengguna (nama, email, password_hash, tanggal_lahir, " +
                     "golongan_usia, golongan_kesibukan, waktu_luang_menit, onboarding_selesai) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNama());
            ps.setString(2, p.getEmail());
            ps.setString(3, p.getPasswordHash());
            if (p.getTanggalLahir() != null) {
                ps.setDate(4, Date.valueOf(p.getTanggalLahir()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setString(5, p.getGolonganUsia() != null ? p.getGolonganUsia() : "dewasa");
            ps.setString(6, p.getGolonganKesibukan() != null ? p.getGolonganKesibukan() : "luar_rumah");
            ps.setInt(7, p.getWaktuLuangMenit() > 0 ? p.getWaktuLuangMenit() : 30);
            ps.setBoolean(8, p.isOnboardingSelesai());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    p.setId(generatedId);
                    return generatedId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error pada createPengguna: " + e.getMessage());
            throw e;
        }

        p.setId(inMemoryId);
        return inMemoryId;
    }

    public boolean updatePengguna(Pengguna p) throws SQLException {
        InMemoryDataStore.getInstance().updatePengguna(p);
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "UPDATE pengguna SET nama = ?, tanggal_lahir = ?, golongan_usia = ?, " +
                     "golongan_kesibukan = ?, waktu_luang_menit = ?, onboarding_selesai = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNama());
            if (p.getTanggalLahir() != null) {
                ps.setDate(2, Date.valueOf(p.getTanggalLahir()));
            } else {
                ps.setNull(2, Types.DATE);
            }
            ps.setString(3, p.getGolonganUsia());
            ps.setString(4, p.getGolonganKesibukan());
            ps.setInt(5, p.getWaktuLuangMenit());
            ps.setBoolean(6, p.isOnboardingSelesai());
            ps.setInt(7, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada updatePengguna: " + e.getMessage());
            throw e;
        }
    }

    public boolean updateOnboarding(int penggunaId, int waktuLuangMenit, String kesibukan, String usia, boolean selesai) throws SQLException {
        Pengguna p = getById(penggunaId);
        if (p != null) {
            p.setWaktuLuangMenit(waktuLuangMenit);
            p.setGolonganKesibukan(kesibukan);
            p.setGolonganUsia(usia);
            p.setOnboardingSelesai(selesai);
            InMemoryDataStore.getInstance().updatePengguna(p);
        }

        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "UPDATE pengguna SET waktu_luang_menit = ?, golongan_kesibukan = ?, " +
                     "golongan_usia = ?, onboarding_selesai = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waktuLuangMenit);
            ps.setString(2, kesibukan);
            ps.setString(3, usia);
            ps.setBoolean(4, selesai);
            ps.setInt(5, penggunaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada updateOnboarding: " + e.getMessage());
            throw e;
        }
    }

    public boolean updatePassword(int penggunaId, String newPasswordHash) throws SQLException {
        Pengguna p = getById(penggunaId);
        if (p != null) {
            p.setPasswordHash(newPasswordHash);
            InMemoryDataStore.getInstance().updatePengguna(p);
        }

        if (!DatabaseConfig.isDatabaseAvailable()) {
            return true;
        }

        String sql = "UPDATE pengguna SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, penggunaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database error pada updatePassword: " + e.getMessage());
            throw e;
        }
    }

    private Pengguna mapResultSetToPengguna(ResultSet rs) throws SQLException {
        Pengguna p = new Pengguna();
        p.setId(rs.getInt("id"));
        p.setNama(rs.getString("nama"));

        try {
            p.setEmail(rs.getString("email"));
        } catch (SQLException ignored) {}

        try {
            p.setPasswordHash(rs.getString("password_hash"));
        } catch (SQLException ignored) {}

        Date tgl = rs.getDate("tanggal_lahir");
        if (tgl != null) {
            p.setTanggalLahir(tgl.toLocalDate());
        }
        p.setGolonganUsia(rs.getString("golongan_usia"));
        p.setGolonganKesibukan(rs.getString("golongan_kesibukan"));
        p.setWaktuLuangMenit(rs.getInt("waktu_luang_menit"));

        try {
            p.setOnboardingSelesai(rs.getBoolean("onboarding_selesai"));
        } catch (SQLException ignored) {
            p.setOnboardingSelesai(true);
        }

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toLocalDateTime());
        }
        return p;
    }
}
