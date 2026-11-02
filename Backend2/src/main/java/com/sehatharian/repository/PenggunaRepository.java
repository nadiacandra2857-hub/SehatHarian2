package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.Pengguna;

import java.sql.*;

/**
 * PenggunaRepository menangani akses data dan query JDBC untuk entitas Pengguna.
 * Mendukung autentikasi kredensial, verifikasi email unik, pembaruan profil,
 * serta penyimpanan preferensi onboarding ke MySQL.
 */
public class PenggunaRepository implements AccountRepository {

    @FunctionalInterface
    public interface ConnectionFactory {
        Connection open() throws SQLException;
    }
    private final ConnectionFactory accountConnections;

    public PenggunaRepository() { this(DatabaseConfig::getConnection); }

    /** Connection seam for isolated JDBC contracts; account and owned profile paths use this factory. */
    public PenggunaRepository(ConnectionFactory accountConnections) {
        this.accountConnections = java.util.Objects.requireNonNull(accountConnections);
    }

    public Pengguna getById(int id, SessionAccess access) throws SQLException {
        access.owner(id);
        String sql = "SELECT id, role, nama, email, password_hash, tanggal_lahir, golongan_usia, golongan_kesibukan, waktu_luang_menit, onboarding_selesai, created_at FROM pengguna WHERE id = ?";
        try (Connection conn = OwnedRepository.open(access, accountConnections); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, access.owner(id));
            try (ResultSet rs = access.call(ps::executeQuery)) { return access.result(rs.next() ? mapResultSetToPengguna(rs) : null); }
        }
    }

    @Override
    public Pengguna findByEmail(String email) throws SQLException {
        if (email == null) return null;
        String sql = "SELECT id, role, nama, email, password_hash, tanggal_lahir, golongan_usia, " +
                "golongan_kesibukan, waktu_luang_menit, onboarding_selesai, created_at " +
                "FROM pengguna WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = accountConnections.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Pengguna pengguna = mapResultSetToPengguna(rs);
                if (rs.next()) throw new SQLException("Identitas akun tidak unik.");
                return pengguna;
            }
        }
    }

    @Override
    public boolean existsByEmail(String email) throws SQLException {
        // Share the strict SQL-only lookup, including failure and duplicate handling.
        return findByEmail(email) != null;
    }

    @Override
    public int createPengguna(Pengguna p) throws SQLException {
        WriteTransaction tx = WriteTransaction.accountCreation(accountConnections);
        try {
            String sql = "INSERT INTO pengguna (nama, email, password_hash, tanggal_lahir, " +
                    "golongan_usia, golongan_kesibukan, waktu_luang_menit, onboarding_selesai) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            Connection conn = tx.open();

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getNama());
                ps.setString(2, p.getEmail());
                ps.setString(3, p.getPasswordHash());
                if (p.getTanggalLahir() == null) ps.setNull(4, Types.DATE);
                else ps.setDate(4, Date.valueOf(p.getTanggalLahir()));
                ps.setString(5, p.getGolonganUsia());
                ps.setString(6, p.getGolonganKesibukan());
                ps.setInt(7, p.getWaktuLuangMenit());
                ps.setBoolean(8, p.isOnboardingSelesai());
                if (tx.execute(ps) != 1) throw new SQLException("Akun belum tersimpan.");
                int id;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next() || (id = rs.getInt(1)) <= 0) {
                        throw new SQLException("ID akun belum dikonfirmasi.");
                    }
                }
                return tx.confirm(id);
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean updatePengguna(Pengguna p, SessionAccess access) throws SQLException {
        access.owner(p.getId());

        WriteTransaction tx = new WriteTransaction(access, accountConnections);
        try {
            String sql = "UPDATE pengguna SET nama = ?, tanggal_lahir = ?, golongan_usia = ?, " +
                         "golongan_kesibukan = ?, waktu_luang_menit = ?, onboarding_selesai = ? WHERE id = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
                ps.setInt(7, access.owner(p.getId()));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean updateOnboarding(int penggunaId, int waktuLuangMenit, String kesibukan, String usia, boolean selesai, SessionAccess access) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, accountConnections);
        try {
            String sql = "UPDATE pengguna SET waktu_luang_menit = ?, golongan_kesibukan = ?, " +
                         "golongan_usia = ?, onboarding_selesai = ? WHERE id = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, waktuLuangMenit);
                ps.setString(2, kesibukan);
                ps.setString(3, usia);
                ps.setBoolean(4, selesai);
                ps.setInt(5, access.owner(penggunaId));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean updatePassword(int penggunaId, String newPasswordHash, SessionAccess access) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, accountConnections);
        try {
            String sql = "UPDATE pengguna SET password_hash = ? WHERE id = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newPasswordHash);
                ps.setInt(2, access.owner(penggunaId));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    private Pengguna mapResultSetToPengguna(ResultSet rs) throws SQLException {
        Pengguna p = new Pengguna();
        p.setId(rs.getInt("id"));
        p.setRole(rs.getString("role"));
        p.setNama(rs.getString("nama"));

        p.setEmail(rs.getString("email"));
        p.setPasswordHash(rs.getString("password_hash"));

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
