package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.RiwayatTantangan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RiwayatTantanganRepository extends OwnedRepository {
    public RiwayatTantanganRepository(SessionAccess access) { super(access); }
    public RiwayatTantanganRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<RiwayatTantangan> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT r.id, r.pengguna_id, r.tantangan_id, r.tanggal, r.durasi_target_menit, " +
                     "r.status, r.diselesaikan_pada, r.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan r " +
                     "JOIN tantangan t ON r.tantangan_id = t.id " +
                     "WHERE r.pengguna_id = ? AND r.tanggal = ? " +
                     "ORDER BY r.id ASC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToRiwayat(rs));
                }
            }
            return access.result(list);
        }
    }

    public List<RiwayatTantangan> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        access.owner(penggunaId);

        List<RiwayatTantangan> list = new ArrayList<>();
        String sql = "SELECT r.id, r.pengguna_id, r.tantangan_id, r.tanggal, r.durasi_target_menit, " +
                     "r.status, r.diselesaikan_pada, r.created_at, " +
                     "t.judul AS tantangan_judul, t.deskripsi AS tantangan_deskripsi, t.kategori AS tantangan_kategori " +
                     "FROM riwayat_tantangan r " +
                     "JOIN tantangan t ON r.tantangan_id = t.id " +
                     "WHERE r.pengguna_id = ? AND r.tanggal BETWEEN ? AND ? " +
                     "ORDER BY r.tanggal ASC, r.id ASC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToRiwayat(rs));
                }
            }
            return access.result(list);
        }
    }

    public int tambah(RiwayatTantangan rt) throws SQLException {
        access.owner(rt.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "INSERT INTO riwayat_tantangan (pengguna_id, tantangan_id, tanggal, durasi_target_menit, status) " +
                         "VALUES (?, ?, ?, ?, ?)";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, access.owner());
                ps.setInt(2, rt.getTantanganId());
                ps.setDate(3, Date.valueOf(rt.getTanggal()));
                ps.setInt(4, rt.getDurasiTargetMenit());
                ps.setString(5, rt.getStatus() != null ? rt.getStatus() : "disarankan");

                if (tx.execute(ps) <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int genId = rs.getInt(1);
                        if (genId <= 0) throw new SQLException("ID penyimpanan belum dikonfirmasi.");
                        return tx.confirm(genId);
                    }
                }
            }
            throw new SQLException("Penyimpanan belum dikonfirmasi.");
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean updateStatus(int id, String status, int penggunaId) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "UPDATE riwayat_tantangan SET status = ?, diselesaikan_pada = ? WHERE id = ? AND pengguna_id = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                if ("selesai".equalsIgnoreCase(status)) {
                    ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                ps.setInt(3, id);
                ps.setInt(4, access.owner(penggunaId));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean existsForDate(int penggunaId, int tantanganId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        String sql = "SELECT COUNT(*) FROM riwayat_tantangan WHERE pengguna_id = ? AND tantangan_id = ? AND tanggal = ?";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setInt(2, tantanganId);
            ps.setDate(3, Date.valueOf(tanggal));
            try (ResultSet rs = access.call(ps::executeQuery)) {
                if (rs.next()) {
                    return access.result(rs.getInt(1) > 0);
                }
            }
        }
        throw new SQLException("Pembacaan belum dikonfirmasi.");
    }

    public boolean deleteByPenggunaAndTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "DELETE FROM riwayat_tantangan WHERE pengguna_id = ? AND tanggal = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, penggunaId);
                ps.setDate(2, Date.valueOf(tanggal));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
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
