package com.sehatharian.nutrition;
import java.nio.file.*;
import java.util.*;
import com.sehatharian.config.DatabaseConfig;

/** Trusted local provisioning command. Upsert stable source IDs, all entries in one transaction. */
public final class FoodImport {
    public static void main(String[] args)throws Exception {
        Properties p=new Properties();try(var in=Files.newInputStream(Path.of(args.length>0?args[0]:".runtime/foods.xml"))){p.loadFromXML(in);}
        int count=Integer.parseInt(p.getProperty("count"));if(count<1||count>50000)throw new IllegalArgumentException();
        try(var conn=DatabaseConfig.getConnection()){
            conn.setAutoCommit(false);
            try(var ps=conn.prepareStatement("INSERT INTO food_catalog(food_id,name,category,nutrients,source,version,license) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),category=VALUES(category),nutrients=VALUES(nutrients),source=VALUES(source),version=VALUES(version),license=VALUES(license)")){
                for(int i=0;i<count;i++){
                    Map<String,Double> vals=new HashMap<>();for(String k:Nutrients.KEYS){String v=p.getProperty(i+"."+k);if(v!=null&&!v.isBlank())vals.put(k,Double.valueOf(v));}
                    ps.setString(1,"tempolife:"+p.getProperty(i+".id"));ps.setString(2,p.getProperty(i+".name"));ps.setString(3,p.getProperty(i+".category"));ps.setString(4,new Nutrients(vals).encode());
                    ps.setString(5,p.getProperty("source")+" | upstream: "+p.getProperty(i+".source","")+" | Food nutrition data from TempoLife (tempolife.app), CC-BY-4.0.");
                    ps.setString(6,p.getProperty("version"));ps.setString(7,p.getProperty("license"));ps.addBatch();
                }ps.executeBatch();conn.commit();System.out.println(count+" katalog diimpor; ID stabil, tanpa duplikasi.");
            }catch(Exception e){conn.rollback();throw e;}
        }
    }
}
