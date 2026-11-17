package com.sehatharian;
import com.sehatharian.config.*;
import java.sql.*;
import java.nio.file.*;
/** Simulates an older installation with no role column, explicit legacy schedules, and existing private records. */
public final class RombakLegacyMigration {
 public static void main(String[] args)throws Exception {
  String name=System.getenv("DB_NAME");if(name==null||!name.matches("sehatharian_test_rombak_legacy_[a-z0-9_]+"))throw new SecurityException();
  String url=DatabaseConfig.getJdbcUrl().replace('/'+name+'?',"/mysql?");
  try(var c=DriverManager.getConnection(url,"root","");var s=c.createStatement()){
   String sql=Files.readString(Path.of("Backend2/sql/schema.sql")).replace("sehatharian_db",name).replace("    role ENUM('user', 'admin') NOT NULL DEFAULT 'user',","").replaceAll("(?m)--[^\\r\\n]*","");
   for(String command:sql.split(";"))if(!command.isBlank())s.execute(command);
   s.execute("INSERT INTO pengguna(id,nama,email,password_hash) VALUES(1,'Legacy Fixture','legacy@example.test','unusable-fixture-hash')");
   s.execute("INSERT INTO jadwal_tidur(pengguna_id,tanggal,jam_tidur,jam_bangun,durasi_jam) VALUES(1,'2026-01-01','22:00','06:00',8)");
   s.execute("INSERT INTO aktivitas(pengguna_id,tanggal,jenis_aktivitas,durasi_menit) VALUES(1,'2026-01-01','Catatan lama',17)");
   s.execute("INSERT INTO kebiasaan_makan(pengguna_id,tanggal,waktu_makan,menu) VALUES(1,'2026-01-01','sarapan','Menu legacy')");
  }
  DatabaseBootstrap.initialize();DatabaseBootstrap.initialize();
  try(var c=DatabaseConfig.getConnection();var s=c.createStatement()){
   String[] checks={"SELECT COUNT(*)=1 FROM pengguna WHERE role='user' AND email='legacy@example.test'","SELECT COUNT(*)=1 FROM jadwal_tidur WHERE durasi_jam=8","SELECT COUNT(*)=1 FROM aktivitas WHERE durasi_menit=17","SELECT COUNT(*)=1 FROM kebiasaan_makan WHERE menu='Menu legacy'","SELECT COUNT(*)=0 FROM sleep_actual","SELECT COUNT(*)=0 FROM food_snapshot"};
   for(String q:checks)try(var r=s.executeQuery(q)){r.next();if(!r.getBoolean(1))throw new AssertionError(q);System.out.println("PASS "+q);}
  }System.out.println("ALL 6 LEGACY MIGRATION CHECKS PASSED");
 }
}
