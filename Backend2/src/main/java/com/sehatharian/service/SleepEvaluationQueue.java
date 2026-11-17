package com.sehatharian.service;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
/** Durable request and invalidation are part of the actual-sleep transaction. */
public final class SleepEvaluationQueue {
 private SleepEvaluationQueue(){}
 public static void lock(Connection c,SessionAccess access)throws SQLException{try(var p=c.prepareStatement("SELECT id FROM pengguna WHERE id=? FOR UPDATE")){p.setInt(1,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new SecurityException();}}}
 /** Legacy trigger field reflects the sum of all episodes on this wake date, never a single episode. */
 public static void dailyTrigger(Connection c,WriteTransaction tx,SessionAccess a,LocalDate day)throws SQLException{
  long seconds=0;
  try(var p=c.prepareStatement("SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND,started_at,ended_at)),0) FROM sleep_actual WHERE pengguna_id=? AND ended_at>=? AND ended_at<?")){
   p.setInt(1,a.owner());p.setObject(2,day.atStartOfDay());p.setObject(3,day.plusDays(1).atStartOfDay());try(var r=p.executeQuery()){r.next();seconds=r.getLong(1);}
  }
  try(var p=c.prepareStatement("UPDATE sleep_actual SET trigger_name=? WHERE pengguna_id=? AND ended_at>=? AND ended_at<?")){
   p.setString(1,seconds>0&&seconds<18000?"tidur_kurang":null);p.setInt(2,a.owner());p.setObject(3,day.atStartOfDay());p.setObject(4,day.plusDays(1).atStartOfDay());tx.execute(p);
  }
 }
 public static void changed(Connection c,WriteTransaction tx,SessionAccess a,LocalDate day)throws SQLException{
  dailyTrigger(c,tx,a,day);
  try(var p=c.prepareStatement("UPDATE sleep_evaluation_result r JOIN sleep_evaluation_request q ON q.result_id=r.id AND q.pengguna_id=r.pengguna_id SET r.stale=TRUE WHERE q.pengguna_id=? AND q.wake_day BETWEEN ? AND ?")){p.setInt(1,a.owner());p.setObject(2,day);p.setObject(3,day.plusDays(6));tx.execute(p);}
  try(var p=c.prepareStatement("UPDATE sleep_evaluation_request SET revision=revision+1,state='PENDING',source_hash=NULL,result_id=NULL,message='Catatan sumber berubah; penilaian perlu diperiksa ulang.',updated_at=? WHERE pengguna_id=? AND wake_day BETWEEN ? AND ?")){p.setObject(1,LocalDateTime.now(ZoneOffset.UTC));p.setInt(2,a.owner());p.setObject(3,day);p.setObject(4,day.plusDays(6));tx.execute(p);}
  try(var p=c.prepareStatement("INSERT IGNORE INTO sleep_evaluation_request(pengguna_id,wake_day,revision,state,message,updated_at) VALUES(?,?,1,'PENDING','Menunggu penilaian setelah catatan tersimpan.',?)")){p.setInt(1,a.owner());p.setObject(2,day);p.setObject(3,LocalDateTime.now(ZoneOffset.UTC));tx.execute(p);}
 }
}
