package com.sehatharian.service;
import com.sehatharian.repository.*;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
public final class HabitPreferences extends OwnedRepository {
    public record Preferences(Integer hydration,String diet,String avoid){}
    public HabitPreferences(SessionAccess access){super(access);}
    public Preferences get()throws SQLException {try(var c=open();var p=c.prepareStatement("SELECT hydration_ml,diet,avoid_foods FROM habit_preferences WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){return access.result(r.next()?new Preferences(r.getObject(1)==null?null:r.getInt(1),r.getString(2),r.getString(3)):new Preferences(null,null,null));}}}
    public boolean save(Integer target,String diet,String avoid)throws SQLException {
        if(target!=null&&(target<250||target>10000))throw new IllegalArgumentException("Target pilihan harus 250–10.000 ml.");
        if(diet!=null&&diet.length()>255||avoid!=null&&avoid.length()>2000)throw new IllegalArgumentException("Preferensi terlalu panjang.");
        WriteTransaction tx=new WriteTransaction(access,connections);try{var c=tx.open();com.sehatharian.nutrition.MenuStore.lockOwner(c,access);
            boolean changed=diet!=null||avoid!=null;try(var old=c.prepareStatement("SELECT diet,avoid_foods FROM habit_preferences WHERE pengguna_id=?")){old.setInt(1,access.owner());try(var r=old.executeQuery()){if(r.next())changed=!java.util.Objects.equals(diet,r.getString(1))||!java.util.Objects.equals(avoid,r.getString(2));}}
            if(changed)com.sehatharian.nutrition.MenuPreferences.invalidateLegacy(c,tx,access);
            try(var p=c.prepareStatement("INSERT INTO habit_preferences(pengguna_id,hydration_ml,diet,avoid_foods) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE hydration_ml=VALUES(hydration_ml),diet=VALUES(diet),avoid_foods=VALUES(avoid_foods)")){
            p.setInt(1,access.owner());p.setObject(2,target);p.setString(3,diet);p.setString(4,avoid);tx.execute(p);return tx.confirm(true);}
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}}
}
