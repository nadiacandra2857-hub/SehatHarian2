package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.EvaluasiMingguan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluasiMingguanRepository extends OwnedRepository {
    public EvaluasiMingguanRepository(SessionAccess access) { super(access); }
    public EvaluasiMingguanRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<EvaluasiMingguan> getAllByPengguna(int penggunaId) throws SQLException {
        access.owner(penggunaId);

        List<EvaluasiMingguan> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, " +
                     "memenuhi_standar_tidur, ringkasan, rekomendasi, dibuat_pada " +
                     "FROM evaluasi_mingguan WHERE pengguna_id = ? ORDER BY minggu_mulai DESC, id DESC";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToEvaluasi(rs));
                }
            }
            return access.result(list);
        }
    }

    public EvaluasiMingguan getTerbaruByPengguna(int penggunaId) throws SQLException {
        access.owner(penggunaId);
        List<EvaluasiMingguan> list = getAllByPengguna(penggunaId);
        return access.result(list.isEmpty() ? null : list.get(0));
    }

    public int simpan(EvaluasiMingguan em) throws SQLException {
        int owner = access.owner(em.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {
            Connection conn = tx.open();

            int existing = em.getId();
            if (existing <= 0) {
                try (PreparedStatement find = conn.prepareStatement("SELECT id FROM evaluasi_mingguan WHERE pengguna_id = ? AND minggu_mulai = ? ORDER BY id DESC LIMIT 1")) {
                    find.setInt(1, owner); find.setDate(2, Date.valueOf(em.getMingguMulai()));
                    try (ResultSet rows = access.call(find::executeQuery)) { if (rows.next()) existing = rows.getInt("id"); }
                }
            }
            if (existing > 0) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE evaluasi_mingguan SET minggu_mulai = ?, skor_konsistensi = ?, durasi_tidur_rata_rata = ?, memenuhi_standar_tidur = ?, ringkasan = ?, rekomendasi = ? WHERE id = ? AND pengguna_id = ?")) {
                ps.setDate(1, Date.valueOf(em.getMingguMulai())); ps.setDouble(2, em.getSkorKonsistensi());
                if (em.getDurasiTidurRataRata() == null) ps.setNull(3, Types.DECIMAL); else ps.setDouble(3, em.getDurasiTidurRataRata());
                ps.setBoolean(4, em.isMemenuhiStandarTidur()); ps.setString(5, em.getRingkasan()); ps.setString(6, em.getRekomendasi());
                    ps.setInt(7, existing); ps.setInt(8, owner);
                    if (tx.execute(ps) <= 0) throw new SQLException("Data tidak tersedia atau tidak berubah.");
                    return tx.confirm(existing);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO evaluasi_mingguan (pengguna_id, minggu_mulai, skor_konsistensi, durasi_tidur_rata_rata, memenuhi_standar_tidur, ringkasan, rekomendasi) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, owner); ps.setDate(2, Date.valueOf(em.getMingguMulai())); ps.setDouble(3, em.getSkorKonsistensi());
                if (em.getDurasiTidurRataRata() == null) ps.setNull(4, Types.DECIMAL); else ps.setDouble(4, em.getDurasiTidurRataRata());
                ps.setBoolean(5, em.isMemenuhiStandarTidur()); ps.setString(6, em.getRingkasan()); ps.setString(7, em.getRekomendasi());
                if (tx.execute(ps) != 1) throw new SQLException("Penyimpanan belum dikonfirmasi.");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next() && keys.getInt(1) > 0) return tx.confirm(keys.getInt(1));
                }
                throw new SQLException("ID penyimpanan belum dikonfirmasi.");
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    private EvaluasiMingguan mapResultSetToEvaluasi(ResultSet rs) throws SQLException {
        EvaluasiMingguan em = new EvaluasiMingguan();
        em.setId(rs.getInt("id"));
        em.setPenggunaId(rs.getInt("pengguna_id"));
        em.setMingguMulai(rs.getDate("minggu_mulai").toLocalDate());
        em.setSkorKonsistensi(rs.getDouble("skor_konsistensi"));

        double tidur = rs.getDouble("durasi_tidur_rata_rata");
        if (!rs.wasNull()) {
            em.setDurasiTidurRataRata(tidur);
        } else {
            em.setDurasiTidurRataRata(null);
        }

        em.setMemenuhiStandarTidur(rs.getBoolean("memenuhi_standar_tidur"));
        em.setRingkasan(rs.getString("ringkasan"));
        em.setRekomendasi(rs.getString("rekomendasi"));

        Timestamp ts = rs.getTimestamp("dibuat_pada");
        if (ts != null) {
            em.setDibuatPada(ts.toLocalDateTime());
        }
        return em;
    }
}
