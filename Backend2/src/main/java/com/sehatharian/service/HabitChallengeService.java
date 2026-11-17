package com.sehatharian.service;
import com.sehatharian.content.ContentService;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Challenge completion reads stored records, never timer state or an unchecked UI flag. */
public final class HabitChallengeService extends OwnedRepository {
    public record Entry(int id,String title,LocalDate date,String category,double target,String reason,boolean complete){}
    public HabitChallengeService(SessionAccess access){super(access);}
    public int join(int contentId,LocalDate date)throws SQLException {
        access.owner();if(!LocalDate.now(ZoneId.of("Asia/Jakarta")).equals(date))throw new IllegalArgumentException("Pilih tanggal hari ini untuk tantangan baru.");
        var content=new ContentService(access).published(ContentService.Kind.CHALLENGE).stream().filter(c->c.id()==contentId).findFirst().orElseThrow(()->new IllegalArgumentException("Tantangan belum terbit."));
        String[] parts=content.body().split("\\|");var advice=new HabitInsights(access).advise(parts[0],date);
        // Manual choice from a public catalogue is allowed with little data. Do not call it a personal recommendation.
        String reason="Dipilih sendiri dari katalog umum. "+advice.reason();if(reason.length()>255)reason=reason.substring(0,255);
        WriteTransaction tx=new WriteTransaction(access,connections);try{var c=tx.open();try(var p=c.prepareStatement("INSERT INTO habit_challenge(pengguna_id,content_id,tanggal,category,target,reason) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            p.setInt(1,access.owner());p.setInt(2,contentId);p.setDate(3,java.sql.Date.valueOf(date));p.setString(4,parts[0]);p.setDouble(5,Double.parseDouble(parts[1]));p.setString(6,reason);tx.execute(p);try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException();return tx.confirm(r.getInt(1));}}
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    public List<Entry> history(LocalDate end)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT h.*,v.title FROM habit_challenge h JOIN content_version v ON v.id=h.content_id WHERE h.pengguna_id=? AND h.tanggal BETWEEN ? AND ? ORDER BY h.tanggal DESC,h.id DESC")){
            p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(end.minusDays(29)));p.setDate(3,java.sql.Date.valueOf(end));List<Entry> out=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())out.add(new Entry(r.getInt("id"),r.getString("title"),r.getDate("tanggal").toLocalDate(),r.getString("category"),r.getDouble("target"),r.getString("reason"),r.getTimestamp("completed_at")!=null));}return access.result(out);
        }
    }
    public boolean evaluate(int id)throws SQLException {
        WriteTransaction tx=new WriteTransaction(access,connections);try{var c=tx.open();try(var p=c.prepareStatement("SELECT category,target,tanggal FROM habit_challenge WHERE id=? AND pengguna_id=? FOR UPDATE")){
            p.setInt(1,id);p.setInt(2,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Tantangan tidak ditemukan.");String category=r.getString(1);double target=r.getDouble(2);var date=r.getDate(3).toLocalDate();String sql=switch(category){
                case "activity"->"SELECT SUM(durasi_menit) FROM aktivitas WHERE pengguna_id=? AND tanggal=?";
                case "hydration"->"SELECT SUM(jumlah_ml) FROM hidrasi WHERE pengguna_id=? AND tanggal=?";
                case "nutrition"->"SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id=? AND tanggal=?";
                case "sleep"->"SELECT SUM(TIMESTAMPDIFF(MINUTE,started_at,ended_at))/60.0 FROM sleep_actual WHERE pengguna_id=? AND DATE(ended_at)=?";default->throw new IllegalArgumentException();};
                try(var q=c.prepareStatement(sql)){q.setInt(1,access.owner());q.setDate(2,java.sql.Date.valueOf(date));try(var rows=q.executeQuery()){if(!rows.next()||rows.getDouble(1)<target||rows.wasNull())throw new IllegalStateException("Kriteria belum dipenuhi oleh catatan tersimpan.");}}
            }}
            try(var p=c.prepareStatement("UPDATE habit_challenge SET completed_at=COALESCE(completed_at,CURRENT_TIMESTAMP) WHERE id=? AND pengguna_id=?")){p.setInt(1,id);p.setInt(2,access.owner());tx.execute(p);return tx.confirm(true);}
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
}
