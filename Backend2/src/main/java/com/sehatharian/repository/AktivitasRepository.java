package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.Aktivitas;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AktivitasRepository extends OwnedRepository {
    public AktivitasRepository(SessionAccess access) { super(access); }
    public AktivitasRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<Aktivitas> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        List<Aktivitas> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan, created_at " +
                     "FROM aktivitas WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToAktivitas(rs));
                }
            }
            return access.result(list);
        }
    }

    public List<Aktivitas> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        access.owner(penggunaId);

        List<Aktivitas> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan, created_at " +
                     "FROM aktivitas WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal ASC, id ASC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToAktivitas(rs));
                }
            }
            return access.result(list);
        }
    }

    public int simpan(Aktivitas a) throws SQLException {
        access.owner(a.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "INSERT INTO aktivitas (pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan) " +
                         "VALUES (?, ?, ?, ?, ?)";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, access.owner());
                ps.setDate(2, Date.valueOf(a.getTanggal()));
                ps.setString(3, a.getJenisAktivitas());
                ps.setInt(4, a.getDurasiMenit());
                ps.setString(5, a.getKeterangan());

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

    public boolean hapus(int id, int penggunaId) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "DELETE FROM aktivitas WHERE id = ? AND pengguna_id = ?";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, access.owner(penggunaId));
                return tx.affected(tx.execute(ps));
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public int getTotalDurasiHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        String sql = "SELECT COALESCE(SUM(durasi_menit), 0) FROM aktivitas WHERE pengguna_id = ? AND tanggal = ?";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = access.call(ps::executeQuery)) {
                if (rs.next()) {
                    return access.result(rs.getInt(1));
                }
            }
        }
        throw new SQLException("Pembacaan belum dikonfirmasi.");
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
