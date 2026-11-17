package com.sehatharian.repository;

import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.JadwalTidur;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JadwalTidurRepository extends OwnedRepository {
    public JadwalTidurRepository(SessionAccess access) { super(access); }
    public JadwalTidurRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        super(access, connections);
    }

    public List<JadwalTidur> getRiwayatByPengguna(int penggunaId, int limit) throws SQLException {
        access.owner(penggunaId);

        List<JadwalTidur> list = new ArrayList<>();
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, " +
                     "memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? " +
                     "ORDER BY tanggal DESC, id DESC" + (limit > 0 ? " LIMIT " + limit : "");

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);

            try (ResultSet rs = access.call(ps::executeQuery)) {
                while (rs.next()) {
                    list.add(mapResultSetToJadwalTidur(rs));
                }
            }
            return access.result(list);
        }
    }

    public List<JadwalTidur> getByRange(int penggunaId, LocalDate start, LocalDate end) throws SQLException {
        access.owner(penggunaId);
        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif, created_at FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal BETWEEN ? AND ? ORDER BY tanggal DESC, id DESC";
        try (Connection conn = open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, access.owner(penggunaId)); ps.setDate(2, Date.valueOf(start)); ps.setDate(3, Date.valueOf(end));
            List<JadwalTidur> list = new ArrayList<>();
            try (ResultSet rows = access.call(ps::executeQuery)) { while (rows.next()) list.add(mapResultSetToJadwalTidur(rows)); }
            return access.result(list);
        }
    }

    public JadwalTidur getTerbaruByPengguna(int penggunaId) throws SQLException {
        access.owner(penggunaId);
        try(var c=open();var p=c.prepareStatement("SELECT schedule_id FROM sleep_active_plan WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){if(r.next()){if(r.getObject(1)==null)return access.result(null);try(var q=c.prepareStatement("SELECT * FROM jadwal_tidur WHERE id=? AND pengguna_id=?")){q.setInt(1,r.getInt(1));q.setInt(2,access.owner());try(var rows=q.executeQuery()){return access.result(rows.next()?mapResultSetToJadwalTidur(rows):null);}}}}}
        List<JadwalTidur> list = getRiwayatByPengguna(penggunaId, 1);
        return access.result(list.isEmpty() ? null : list.get(0));
    }

    public JadwalTidur getByTanggal(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);

        String sql = "SELECT id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, " +
                     "memenuhi_standar, pengingat_aktif, created_at " +
                     "FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal = ? LIMIT 1";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.setDate(2, Date.valueOf(tanggal));

            try (ResultSet rs = access.call(ps::executeQuery)) {
                if (rs.next()) {
                    return mapResultSetToJadwalTidur(rs);
                }
            }
        }
        throw new SQLException("Pembacaan belum dikonfirmasi.");
    }

    public int simpan(JadwalTidur jt) throws SQLException {
        int owner = access.owner(jt.getPenggunaId());

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {
            Connection conn = tx.open();
            com.sehatharian.service.SleepEvaluationQueue.lock(conn,access);

            int existing = jt.getId();
            if (existing <= 0) {
                try (PreparedStatement find = conn.prepareStatement("SELECT id FROM jadwal_tidur WHERE pengguna_id = ? AND tanggal = ? ORDER BY id DESC LIMIT 1")) {
                    find.setInt(1, owner); find.setDate(2, Date.valueOf(jt.getTanggal()));
                    try (ResultSet rows = access.call(find::executeQuery)) { if (rows.next()) existing = rows.getInt("id"); }
                }
            }
            if (existing > 0) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE jadwal_tidur SET tanggal = ?, jam_tidur = ?, jam_bangun = ?, durasi_jam = ?, memenuhi_standar = ?, pengingat_aktif = ? WHERE id = ? AND pengguna_id = ?")) {
                ps.setDate(1, Date.valueOf(jt.getTanggal()));
                ps.setTime(2, Time.valueOf(jt.getJamTidur())); ps.setTime(3, Time.valueOf(jt.getJamBangun()));
                ps.setDouble(4, jt.getDurasiJam()); ps.setBoolean(5, jt.isMemenuhiStandar()); ps.setBoolean(6, jt.isPengingatAktif());
                    ps.setInt(7, existing); ps.setInt(8, owner);
                    if (tx.execute(ps) <= 0) throw new SQLException("Data tidak tersedia atau tidak berubah.");
                    selectActive(conn,tx,existing); return tx.confirm(existing);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO jadwal_tidur (pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, owner); ps.setDate(2, Date.valueOf(jt.getTanggal()));
                ps.setTime(3, Time.valueOf(jt.getJamTidur())); ps.setTime(4, Time.valueOf(jt.getJamBangun()));
                ps.setDouble(5, jt.getDurasiJam()); ps.setBoolean(6, jt.isMemenuhiStandar()); ps.setBoolean(7, jt.isPengingatAktif());
                if (tx.execute(ps) != 1) throw new SQLException("Penyimpanan belum dikonfirmasi.");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next() && keys.getInt(1) > 0) {int id=keys.getInt(1);selectActive(conn,tx,id);return tx.confirm(id);}
                }
                throw new SQLException("ID penyimpanan belum dikonfirmasi.");
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean setPengingatAktif(int id, boolean aktif, int penggunaId) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "UPDATE jadwal_tidur SET pengingat_aktif = ? WHERE id = ? AND pengguna_id = ?";
            Connection conn = tx.open();
            com.sehatharian.service.SleepEvaluationQueue.lock(conn,access);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, aktif);
                ps.setInt(2, id);
                ps.setInt(3, access.owner(penggunaId));
                int changed=tx.execute(ps);
                try(var bump=conn.prepareStatement("UPDATE sleep_active_plan SET generation=generation+1 WHERE pengguna_id=? AND schedule_id=?")){bump.setInt(1,access.owner());bump.setInt(2,id);tx.execute(bump);}
                return tx.affected(changed);
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    public boolean hapus(int id, int penggunaId) throws SQLException {
        access.owner(penggunaId);

        WriteTransaction tx = new WriteTransaction(access, connections);
        try {

            String sql = "DELETE FROM jadwal_tidur WHERE id = ? AND pengguna_id = ?";
            Connection conn = tx.open();
            com.sehatharian.service.SleepEvaluationQueue.lock(conn,access);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, access.owner(penggunaId));
                int changed=tx.execute(ps);
                try(var bump=conn.prepareStatement("UPDATE sleep_active_plan SET generation=generation+1 WHERE pengguna_id=? AND schedule_id=?")){bump.setInt(1,access.owner());bump.setInt(2,id);tx.execute(bump);}
                return tx.affected(changed);
            }
        } catch (SQLException e) { return tx.recover(e); }
        catch (RuntimeException e) { tx.abort(); throw e; }
        finally { tx.close(); }
    }

    private void selectActive(Connection c,WriteTransaction tx,int id)throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO sleep_active_plan(pengguna_id,schedule_id,generation) VALUES(?,?,1) ON DUPLICATE KEY UPDATE schedule_id=VALUES(schedule_id),generation=generation+1")){p.setInt(1,access.owner());p.setInt(2,id);tx.execute(p);}
    }
    private JadwalTidur mapResultSetToJadwalTidur(ResultSet rs) throws SQLException {
        JadwalTidur jt = new JadwalTidur();
        jt.setId(rs.getInt("id"));
        jt.setPenggunaId(rs.getInt("pengguna_id"));
        jt.setTanggal(rs.getDate("tanggal").toLocalDate());
        jt.setJamTidur(rs.getTime("jam_tidur").toLocalTime());
        jt.setJamBangun(rs.getTime("jam_bangun").toLocalTime());
        jt.setDurasiJam(rs.getDouble("durasi_jam"));
        jt.setMemenuhiStandar(rs.getBoolean("memenuhi_standar"));
        jt.setPengingatAktif(rs.getBoolean("pengingat_aktif"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            jt.setCreatedAt(ts.toLocalDateTime());
        }
        return jt;
    }
}
