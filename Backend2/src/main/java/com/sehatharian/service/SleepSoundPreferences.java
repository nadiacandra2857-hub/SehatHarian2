package com.sehatharian.service;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
public final class SleepSoundPreferences extends OwnedRepository {
 public record Choice(boolean enabled,double volume){}
 public SleepSoundPreferences(SessionAccess a){super(a);}
 public SleepSoundPreferences(SessionAccess a,PenggunaRepository.ConnectionFactory f){super(a,f);}
 public Choice get()throws SQLException{try(var c=open();var p=c.prepareStatement("SELECT enabled,volume FROM sleep_sound_preference WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){return access.result(r.next()?new Choice(r.getBoolean(1),r.getDouble(2)):new Choice(false,.35));}}}
 public boolean save(boolean enabled,double volume)throws SQLException{if(!Double.isFinite(volume)||volume<0||volume>1)throw new IllegalArgumentException();var tx=new WriteTransaction(access,connections);try{var c=tx.open();SleepEvaluationQueue.lock(c,access);try(var p=c.prepareStatement("INSERT INTO sleep_sound_preference(pengguna_id,enabled,volume) VALUES(?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),volume=VALUES(volume)")){p.setInt(1,access.owner());p.setBoolean(2,enabled);p.setDouble(3,volume);tx.execute(p);return tx.confirm(true);}}catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}}
}
