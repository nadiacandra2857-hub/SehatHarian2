package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.KebiasaanMakan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KebiasaanMakanRepository extends OwnedRepository {
    public KebiasaanMakanRepository(SessionAccess access) { super(access); }
    public KebiasaanMakanRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<KebiasaanMakan> getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        List<KebiasaanMakan> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, waktu_makan, menu, keterangan, created_at " +
                     "FROM kebiasaan_makan WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToMakan(rs));
                }
            }
            return access.result(list);
        }
    }

    public int simpan(KebiasaanMakan km) throws SQLException {
        access.owner(km.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "INSERT INTO kebiasaan_makan (pengguna_id, tanggal, waktu_makan, menu, keterangan) " +
                         "VALUES (?, ?, ?, ?, ?)";
            Connection conn = tx.open();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, access.owner());
                ps.setDate(2, Date.valueOf(km.getTanggal()));
                ps.setString(3, km.getWaktuMakan());
                ps.setString(4, km.getMenu());
                ps.setString(5, km.getKeterangan());

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

            String sql = "DELETE FROM kebiasaan_makan WHERE id = ? AND pengguna_id = ?";
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
