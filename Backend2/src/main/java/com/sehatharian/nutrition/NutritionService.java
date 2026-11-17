package com.sehatharian.nutrition;
import com.sehatharian.repository.*;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class NutritionService extends OwnedRepository {
    public record Food(String id,String name,String category,Nutrients nutrients,String source,String version) { @Override public String toString(){return name;} }
    public record Meal(int id,String name,String time,Double grams,Nutrients nutrients,String source,String version,Double portions,String details) {
        public Meal(int id,String name,String time,Double grams,Nutrients nutrients,String source,String version){this(id,name,time,grams,nutrients,source,version,null,null);}
    }
    public NutritionService(SessionAccess access){super(access);}
    public NutritionService(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public List<Food> search(String query)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT * FROM food_catalog WHERE name LIKE ? ORDER BY name LIMIT 40")){
            p.setString(1,"%"+query.strip()+"%");List<Food> out=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())out.add(new Food(r.getString("food_id"),r.getString("name"),r.getString("category"),Nutrients.decode(r.getString("nutrients")),r.getString("source"),r.getString("version")));}return access.result(out);
        }
    }
    public int save(LocalDate day,String mealTime,String foodId,double grams)throws SQLException {return save(day,mealTime,foodId,grams,null);}
    public int save(LocalDate day,String mealTime,String foodId,double grams,String expectedVersion)throws SQLException {
        new Nutrients(Map.of()).portion(grams);
        if(day==null||!Set.of("sarapan","makan_siang","makan_malam","camilan").contains(mealTime))throw new IllegalArgumentException("Tanggal dan waktu makan wajib diisi.");
        WriteTransaction tx=new WriteTransaction(access,connections);
        try{
            var c=tx.open();Food f;
            try(var p=c.prepareStatement("SELECT * FROM food_catalog WHERE food_id=?")){p.setString(1,foodId);try(var r=p.executeQuery()){
                if(!r.next())throw new IllegalArgumentException("Item katalog tidak tersedia.");
                f=new Food(r.getString("food_id"),r.getString("name"),r.getString("category"),Nutrients.decode(r.getString("nutrients")),r.getString("source"),r.getString("version"));}}
            if(expectedVersion!=null&&!expectedVersion.equals(f.version()))throw new IllegalStateException("Katalog berubah sejak tinjauan. Cari dan tinjau ulang sebelum menyimpan.");
            int id;try(var p=c.prepareStatement("INSERT INTO kebiasaan_makan(pengguna_id,tanggal,waktu_makan,menu,keterangan) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(day));p.setString(3,mealTime);p.setString(4,f.name().substring(0,Math.min(150,f.name().length())));p.setString(5,"Porsi "+grams+" g; estimasi katalog; snapshot sumber tersimpan");if(tx.execute(p)!=1)throw new SQLException();try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException();id=r.getInt(1);}}
            try(var p=c.prepareStatement("INSERT INTO food_snapshot(meal_id,pengguna_id,grams,nutrients,food_id,source,version) VALUES(?,?,?,?,?,?,?)")){
                p.setInt(1,id);p.setInt(2,access.owner());p.setDouble(3,grams);p.setString(4,f.nutrients().portion(grams).encode());p.setString(5,f.id());p.setString(6,f.source());p.setString(7,f.version());tx.execute(p);}
            return tx.confirm(id);
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    public List<Meal> history(LocalDate day)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT m.id,m.menu,m.waktu_makan,s.grams,COALESCE(q.nutrients,s.nutrients),COALESCE(q.source,s.source),COALESCE(CONCAT('Menu v',q.content_revision),s.version),q.portions,q.details FROM kebiasaan_makan m LEFT JOIN food_snapshot s ON s.meal_id=m.id AND s.pengguna_id=m.pengguna_id LEFT JOIN menu_meal_snapshot q ON q.meal_id=m.id AND q.pengguna_id=m.pengguna_id WHERE m.pengguna_id=? AND m.tanggal=? ORDER BY m.id DESC")){
            p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(day));List<Meal> out=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())out.add(new Meal(r.getInt(1),r.getString(2),r.getString(3),(Double)(r.getObject(4)==null?null:r.getDouble(4)),Nutrients.decode(r.getString(5)),r.getString(6),r.getString(7),r.getObject(8)==null?null:r.getDouble(8),r.getString(9)));}return access.result(out);
        }
    }
}
