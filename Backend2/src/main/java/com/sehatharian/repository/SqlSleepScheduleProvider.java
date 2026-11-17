package com.sehatharian.repository;
import com.sehatharian.model.*;
import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.service.*;
import com.sehatharian.storage.WriteTransaction;
import java.time.*;
public final class SqlSleepScheduleProvider implements SleepScheduleProvider {
 private final UserSession session;private final PenggunaRepository.ConnectionFactory connections;
 public SqlSleepScheduleProvider(UserSession s){this(s,DatabaseConfig::getConnection);}
 public SqlSleepScheduleProvider(UserSession s,PenggunaRepository.ConnectionFactory c){session=s;connections=c;}
 public Schedule latest(int user)throws java.sql.SQLException {
  var a=new SessionAccess(session);a.owner(user);var j=new JadwalTidurRepository(a,connections).getTerbaruByPengguna(user);
  if(j==null)return a.result(null);
  long generation=0;try(var c=OwnedRepository.open(a,connections);var p=c.prepareStatement("SELECT generation FROM sleep_active_plan WHERE pengguna_id=?")){p.setInt(1,a.owner());try(var r=p.executeQuery()){if(r.next())generation=r.getLong(1);}}
  var sound=new SleepSoundPreferences(a,connections).get();
  return a.result(new Schedule(j.getJamTidur(),j.isPengingatAktif(),j.getTanggal(),j.getJamBangun(),generation+":"+j.getId()+":"+j.getTanggal()+":"+j.getJamTidur()+":"+j.getJamBangun()+":"+j.isPengingatAktif(),sound.enabled(),sound.volume()));
 }
 public boolean claim(int user,Schedule schedule,LocalDateTime occurrence)throws java.sql.SQLException {
  var a=new SessionAccess(session);a.owner(user);var tx=new WriteTransaction(a,connections);try{var c=tx.open();SleepEvaluationQueue.lock(c,a);
   // Re-read under owner lock; a changed/disabled schedule cannot claim an old occurrence.
   var current=latest(user);if(current==null||!current.enabled()||!current.equals(schedule))return tx.confirm(false);
   try(var p=c.prepareStatement("INSERT IGNORE INTO sleep_alarm_claim(pengguna_id,occurrence,kind,schedule_identity,claimed_at) VALUES(?,?,'START',?,?)")){p.setInt(1,a.owner());p.setObject(2,occurrence);p.setString(3,schedule.identity());p.setObject(4,LocalDateTime.now(ZoneOffset.UTC));return tx.confirm(tx.execute(p)==1);}
  }catch(java.sql.SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
 }
}
