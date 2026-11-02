package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.Hidrasi;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HidrasiRepository extends OwnedRepository {
    public HidrasiRepository(SessionAccess access) { super(access); }
    public HidrasiRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<Hidrasi> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToHidrasi(rs));
                }
            }
            return access.result(list);
        }
    }

    public List<Hidrasi> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        access.owner(penggunaId);

        List<Hidrasi> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jumlah_ml, catatan, created_at " +
                     "FROM hidrasi WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal ASC, id ASC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToHidrasi(rs));
                }
            }
            return access.result(list);
        }
    }

    public int simpan(Hidrasi h) throws SQLException {
        access.owner(h.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "INSERT INTO hidrasi (pengguna_id, tanggal, jumlah_ml, catatan) " +
                         "VALUES (?, ?, ?, ?)";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, access.owner());
                ps.setDate(2, Date.valueOf(h.getTanggal()));
                ps.setInt(3, h.getJumlahMl());
                ps.setString(4, h.getCatatan());

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

            String sql = "DELETE FROM hidrasi WHERE id = ? AND pengguna_id = ?";
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

    public int getTotalMlHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        String sql = "SELECT COALESCE(SUM(jumlah_ml), 0) FROM hidrasi WHERE pengguna_id = ? AND tanggal = ?";
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
