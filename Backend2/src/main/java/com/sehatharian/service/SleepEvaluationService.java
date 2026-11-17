package com.sehatharian.service;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
import java.util.*;
/** Durable daily request; historical results survive source revisions. */
public final class SleepEvaluationService extends OwnedRepository {
 public record Result(LocalDate day,long revision,String state,String summary,String details,boolean stale){}
 public SleepEvaluationService(SessionAccess a){super(a);}
 public SleepEvaluationService(SessionAccess a,PenggunaRepository.ConnectionFactory f){super(a,f);}
 public Result process(LocalDate day)throws SQLException {
  if(day==null||day.isAfter(LocalDate.now(PengingatTidurService.ZONE)))throw new IllegalArgumentException("Tanggal penilaian tidak valid.");
  var tx=new WriteTransaction(access,connections);try{var c=tx.open();SleepEvaluationQueue.lock(c,access);SleepEvaluationQueue.dailyTrigger(c,tx,access,day);
   long seconds=0;int episodes=0;var signature=new ArrayList<String>(List.of("sleep-daily-v2",day.toString()));
   String[] queries={"SELECT id,started_at,ended_at FROM sleep_actual WHERE pengguna_id=? AND ended_at>=? AND ended_at<? ORDER BY id", "SELECT onboarding_selesai,waktu_luang_menit,golongan_usia,golongan_kesibukan FROM pengguna WHERE id=?", "SELECT hydration_ml,diet,avoid_foods FROM habit_preferences WHERE pengguna_id=?", "SELECT id,tanggal,jam_tidur,jam_bangun,pengingat_aktif FROM jadwal_tidur WHERE pengguna_id=? ORDER BY id"};
   for(int q=0;q<queries.length;q++)try(var p=c.prepareStatement(queries[q])){p.setInt(1,access.owner());if(q==0){p.setObject(2,day.minusDays(6).atStartOfDay());p.setObject(3,day.plusDays(1).atStartOfDay());}try(var r=p.executeQuery()){while(r.next()){for(int col=1;col<=r.getMetaData().getColumnCount();col++)signature.add(Objects.toString(r.getObject(col),"unknown"));if(q==0&&r.getTimestamp(3).toLocalDateTime().toLocalDate().equals(day)){episodes++;seconds+=Duration.between(r.getTimestamp(2).toLocalDateTime(),r.getTimestamp(3).toLocalDateTime()).getSeconds();}}}}
   String hash=com.sehatharian.nutrition.MenuModels.digest(signature);long revision=1;String oldHash=null;Long oldResult=null;
   try(var p=c.prepareStatement("SELECT revision,source_hash,result_id FROM sleep_evaluation_request WHERE pengguna_id=? AND wake_day=?")){p.setInt(1,access.owner());p.setObject(2,day);try(var r=p.executeQuery()){if(r.next()){revision=r.getLong(1);oldHash=r.getString(2);oldResult=r.getObject(3)==null?null:r.getLong(3);}}}
   if(hash.equals(oldHash)&&oldResult!=null){try(var p=c.prepareStatement("SELECT state,summary,details,stale FROM sleep_evaluation_result WHERE id=? AND pengguna_id=?")){p.setLong(1,oldResult);p.setInt(2,access.owner());try(var r=p.executeQuery()){if(r.next())return tx.confirm(new Result(day,revision,r.getString(1),r.getString(2),r.getString(3),r.getBoolean(4)));}}}
   if(oldHash!=null&&!oldHash.equals(hash)){revision++;try(var p=c.prepareStatement("UPDATE sleep_evaluation_result SET stale=TRUE WHERE pengguna_id=? AND wake_day=?")){p.setInt(1,access.owner());p.setObject(2,day);tx.execute(p);}}
   String state,summary,details;
   String coverage="Jumlah hanya episode tercatat per tanggal bangun WIB; belum menjamin seluruh tidur harian sudah dicatat. Periode "+day.minusDays(6)+"–"+day+". ";
   String context=String.format(Locale.ROOT,"Tanggal bangun %s · %d episode · total %.2f jam aktual. ",day,episodes,seconds/3600.0);
   if(episodes==0){state="NO_DATA";summary="Belum ada tidur aktual pada tanggal ini.";details=coverage+"Durasi belum diketahui; bukan nol jam tidur.";}
   else if(seconds>=18000){state="NO_TRIGGER";summary=context+"Tidak memicu tidur_kurang.";details=coverage+"Ambang internal proyek: total kurang dari 5 jam, bukan ukuran kualitas tidur.";}
   else{var advice=new HabitInsights(access).sleepAdvice(c,day);state=advice.state().equals("READY")?"READY":"WAITING";summary=context+"Di bawah ambang internal 5 jam.";details=coverage+advice.reason();}
   long id;try(var p=c.prepareStatement("INSERT INTO sleep_evaluation_result(pengguna_id,wake_day,revision,source_hash,state,summary,details,created_at) VALUES(?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setInt(1,access.owner());p.setObject(2,day);p.setLong(3,revision);p.setString(4,hash);p.setString(5,state);p.setString(6,summary);p.setString(7,details);p.setObject(8,LocalDateTime.now(ZoneOffset.UTC));tx.execute(p);try(var r=p.getGeneratedKeys()){r.next();id=r.getLong(1);}}
   try(var p=c.prepareStatement("INSERT INTO sleep_evaluation_request(pengguna_id,wake_day,revision,state,source_hash,result_id,message,updated_at) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE revision=VALUES(revision),state=VALUES(state),source_hash=VALUES(source_hash),result_id=VALUES(result_id),message=VALUES(message),updated_at=VALUES(updated_at)")){p.setInt(1,access.owner());p.setObject(2,day);p.setLong(3,revision);p.setString(4,state);p.setString(5,hash);p.setLong(6,id);p.setString(7,summary);p.setObject(8,LocalDateTime.now(ZoneOffset.UTC));tx.execute(p);}
   return tx.confirm(new Result(day,revision,state,summary,details,false));
  }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
 }
 public List<Result> history()throws SQLException {try(var c=open();var p=c.prepareStatement("SELECT wake_day,revision,state,summary,details,stale FROM sleep_evaluation_result WHERE pengguna_id=? ORDER BY id DESC LIMIT 30")){p.setInt(1,access.owner());var out=new ArrayList<Result>();try(var r=p.executeQuery()){while(r.next())out.add(new Result(r.getDate(1).toLocalDate(),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getBoolean(6)));}return access.result(out);}}
 public record Request(LocalDate day,long revision,String state,String message){}
 public List<Request> requests()throws SQLException {try(var c=open();var p=c.prepareStatement("SELECT wake_day,revision,state,message FROM sleep_evaluation_request WHERE pengguna_id=? ORDER BY wake_day DESC LIMIT 30")){p.setInt(1,access.owner());var out=new ArrayList<Request>();try(var r=p.executeQuery()){while(r.next())out.add(new Request(r.getDate(1).toLocalDate(),r.getLong(2),r.getString(3),r.getString(4)));}return access.result(out);}}
 public void pending()throws SQLException {List<LocalDate> days=new ArrayList<>();try(var c=open();var p=c.prepareStatement("SELECT wake_day FROM sleep_evaluation_request WHERE pengguna_id=? AND state IN ('PENDING','WAITING') ORDER BY wake_day DESC LIMIT 30")){p.setInt(1,access.owner());try(var r=p.executeQuery()){while(r.next())days.add(r.getDate(1).toLocalDate());}}for(var day:days)process(day);}
}
