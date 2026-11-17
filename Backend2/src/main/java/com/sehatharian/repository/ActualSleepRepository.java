package com.sehatharian.repository;

import com.sehatharian.model.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Compatibility adapter for existing evaluation interfaces. Only sleep_actual supplies measured sleep. */
public final class ActualSleepRepository extends JadwalTidurRepository {
    public ActualSleepRepository(SessionAccess access){super(access);}
    public ActualSleepRepository(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public record Receipt(Integer id,LocalDateTime start,LocalDateTime end){}
    public Optional<Receipt> receipt(UUID operation)throws SQLException{
        var tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.service.SleepEvaluationQueue.lock(c,access);return tx.confirm(find(c,operation));}catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    private Optional<Receipt> find(Connection c,UUID operation)throws SQLException{
        try(var p=c.prepareStatement("SELECT sleep_id,started_at,ended_at FROM sleep_record_receipt WHERE pengguna_id=? AND operation_id=?")){p.setInt(1,access.owner());p.setString(2,operation.toString());try(var r=p.executeQuery()){return r.next()?Optional.of(new Receipt(r.getObject(1)==null?null:r.getInt(1),r.getTimestamp(2).toLocalDateTime(),r.getTimestamp(3).toLocalDateTime())):Optional.empty();}}
    }
    public int record(LocalDateTime start,LocalDateTime end)throws SQLException{return record(UUID.randomUUID(),start,end);}
    public int record(UUID operation,LocalDateTime start,LocalDateTime end)throws SQLException {
        access.owner();Objects.requireNonNull(operation);duration(start,end);
        var tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.service.SleepEvaluationQueue.lock(c,access);
            var old=find(c,operation);if(old.isPresent()){
                var r=old.get();if(r.id()==null)throw new IllegalStateException("Catatan operasi ini sudah dihapus; tidak dibuat kembali.");
                if(!r.start().equals(start)||!r.end().equals(end))throw new IllegalArgumentException("Operasi tersimpan mempunyai interval berbeda. Periksa penyimpanan dahulu.");return tx.confirm(r.id());
            }
            overlap(c,0,start,end);int id;
            try(var p=c.prepareStatement("INSERT INTO sleep_actual(pengguna_id,started_at,ended_at,trigger_name) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,access.owner());p.setObject(2,start);p.setObject(3,end);p.setString(4,null);tx.execute(p);try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException();id=r.getInt(1);}}
            try(var p=c.prepareStatement("INSERT INTO sleep_record_receipt(pengguna_id,operation_id,sleep_id,started_at,ended_at) VALUES(?,?,?,?,?)")){p.setInt(1,access.owner());p.setString(2,operation.toString());p.setInt(3,id);p.setObject(4,start);p.setObject(5,end);tx.execute(p);}
            com.sehatharian.service.SleepEvaluationQueue.changed(c,tx,access,end.toLocalDate());return tx.confirm(id);
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    private void overlap(Connection c,int excluded,LocalDateTime start,LocalDateTime end)throws SQLException{
        try(var p=c.prepareStatement("SELECT id FROM sleep_actual WHERE pengguna_id=? AND id<>? AND started_at<? AND ended_at>? LIMIT 1")){p.setInt(1,access.owner());p.setInt(2,excluded);p.setObject(3,end);p.setObject(4,start);try(var r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Episode tidur bertumpang tindih dengan catatan tersimpan.");}}
    }
    public int replace(int id,LocalDateTime start,LocalDateTime end)throws SQLException{
        duration(start,end);var tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.service.SleepEvaluationQueue.lock(c,access);LocalDate oldDay;
            try(var p=c.prepareStatement("SELECT started_at,ended_at FROM sleep_actual WHERE id=? AND pengguna_id=?")){p.setInt(1,id);p.setInt(2,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new SecurityException("Catatan tidak tersedia.");if(start.equals(r.getTimestamp(1).toLocalDateTime())&&end.equals(r.getTimestamp(2).toLocalDateTime()))return tx.confirm(id);oldDay=r.getTimestamp(2).toLocalDateTime().toLocalDate();}}
            overlap(c,id,start,end);try(var p=c.prepareStatement("UPDATE sleep_actual SET started_at=?,ended_at=?,trigger_name=? WHERE id=? AND pengguna_id=?")){p.setObject(1,start);p.setObject(2,end);p.setString(3,null);p.setInt(4,id);p.setInt(5,access.owner());tx.execute(p);}
            com.sehatharian.service.SleepEvaluationQueue.changed(c,tx,access,oldDay);if(!oldDay.equals(end.toLocalDate()))com.sehatharian.service.SleepEvaluationQueue.changed(c,tx,access,end.toLocalDate());return tx.confirm(id);
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    public boolean deleteActual(int id)throws SQLException{
        var tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.service.SleepEvaluationQueue.lock(c,access);LocalDate day;
            try(var p=c.prepareStatement("SELECT ended_at FROM sleep_actual WHERE id=? AND pengguna_id=?")){p.setInt(1,id);p.setInt(2,access.owner());try(var r=p.executeQuery()){if(!r.next())return tx.confirm(false);day=r.getTimestamp(1).toLocalDateTime().toLocalDate();}}
            try(var p=c.prepareStatement("DELETE FROM sleep_actual WHERE id=? AND pengguna_id=?")){p.setInt(1,id);p.setInt(2,access.owner());tx.execute(p);}com.sehatharian.service.SleepEvaluationQueue.changed(c,tx,access,day);return tx.confirm(true);
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    @Override public boolean hapus(int id,int user)throws SQLException{access.owner(user);return deleteActual(id);}
    public static long duration(LocalDateTime start,LocalDateTime end) {
        if(start==null||end==null)throw new IllegalArgumentException("Isi tanggal dan jam tidur/bangun.");
        long seconds=Duration.between(start,end).getSeconds();long minutes=seconds/60;if(seconds<1800||seconds>72000)throw new IllegalArgumentException("Tidur aktual harus 30 menit sampai 20 jam; periksa tanggal bangun.");
        if(end.isAfter(LocalDateTime.now(ZoneId.of("Asia/Jakarta")).plusMinutes(1)))throw new IllegalArgumentException("Catatan aktual tidak boleh di masa depan.");return minutes;
    }
    public record Episode(int id,LocalDateTime start,LocalDateTime end){}
    public Optional<Episode> status(int id)throws SQLException{var tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.service.SleepEvaluationQueue.lock(c,access);try(var p=c.prepareStatement("SELECT started_at,ended_at FROM sleep_actual WHERE id=? AND pengguna_id=?")){p.setInt(1,id);p.setInt(2,access.owner());try(var r=p.executeQuery()){return tx.confirm(r.next()?Optional.of(new Episode(id,r.getTimestamp(1).toLocalDateTime(),r.getTimestamp(2).toLocalDateTime())):Optional.empty());}}}catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}}
    public List<Episode> episodes()throws SQLException{try(var c=open();var p=c.prepareStatement("SELECT id,started_at,ended_at FROM sleep_actual WHERE pengguna_id=? ORDER BY ended_at DESC LIMIT 60")){p.setInt(1,access.owner());var out=new ArrayList<Episode>();try(var r=p.executeQuery()){while(r.next())out.add(new Episode(r.getInt(1),r.getTimestamp(2).toLocalDateTime(),r.getTimestamp(3).toLocalDateTime()));}return access.result(out);}}
    @Override public List<JadwalTidur> getByRange(int user,LocalDate from,LocalDate to)throws SQLException {
        access.owner(user);List<JadwalTidur> out=new ArrayList<>();
        try(var c=open();var p=c.prepareStatement("SELECT id,started_at,ended_at FROM sleep_actual WHERE pengguna_id=? AND ended_at>=? AND ended_at<? ORDER BY ended_at DESC,id DESC")){
            p.setInt(1,access.owner(user));p.setTimestamp(2,Timestamp.valueOf(from.atStartOfDay()));p.setTimestamp(3,Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try(var r=p.executeQuery()){while(r.next()){var a=r.getTimestamp(2).toLocalDateTime();var b=r.getTimestamp(3).toLocalDateTime();double hours=Duration.between(a,b).getSeconds()/3600.0;out.add(new JadwalTidur(r.getInt(1),user,b.toLocalDate(),a.toLocalTime(),b.toLocalTime(),hours,hours>=5,false));}}
        }return access.result(out);
    }
    @Override public List<JadwalTidur> getRiwayatByPengguna(int user,int limit)throws SQLException {
        var rows=getByRange(user,LocalDate.of(1900,1,1),LocalDate.now(ZoneId.of("Asia/Jakarta")));return rows.stream().limit(limit>0?limit:Long.MAX_VALUE).toList();
    }
}
