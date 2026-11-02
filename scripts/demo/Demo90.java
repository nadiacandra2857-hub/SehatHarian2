import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.model.*;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.*;
import com.sehatharian.util.PasswordUtil;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit synthetic presentation data, never invoked by production startup. */
public class Demo90 {
    static final String DB = "sehatharian_demo_db", EMAIL = "demo90@sehatharian.local";
    static final String PASSWORD = "Presentasi90!";
    static final LocalDate END = LocalDate.now(ZoneId.of("Asia/Jakarta")), START = END.minusDays(89);
    static void insert(Connection c, String sql, Object... args) throws SQLException {
        try (var s = c.prepareStatement(sql)) {
            for (int i=0;i<args.length;i++) s.setObject(i+1,args[i]);
            if(s.executeUpdate()!=1) throw new SQLException("Insert tidak dikonfirmasi");
        }
    }
    static List<String> statements(String path) throws Exception {
        return Arrays.stream(Files.readString(Path.of(path)).replaceAll("(?m)--[^\\n]*", "").split(";"))
            .map(String::trim).filter(s->!s.isEmpty()).toList();
    }
    public static void main(String[] args) throws Exception {
        if (!DB.equals(System.getenv("DB_NAME")) || !"127.0.0.1".equals(System.getenv("DB_HOST")))
            throw new IllegalStateException("Hanya database demo lokal eksplisit yang diizinkan.");
        if(args.length!=1 || !List.of("--dry-run","--execute","--verify").contains(args[0]))
            throw new IllegalArgumentException("Gunakan --dry-run, --execute atau --verify");
        System.out.println("DATA SINTETIS | " + DB + " | " + START + " s.d. " + END + " (90 hari)");
        if(args[0].equals("--dry-run")) {System.out.println("1 user, 90 aktivitas, 360 hidrasi, 270 makan, 90 tidur, 270 tantangan, 12 evaluasi. Tidak ada koneksi/tulisan DB."); return;}
        Class.forName("com.mysql.cj.jdbc.Driver");
        String port = System.getenv().getOrDefault("DB_PORT","13306");
        String user = System.getenv().getOrDefault("DB_USER","root"), pass=System.getenv().getOrDefault("DB_PASSWORD", "");
        if(args[0].equals("--execute")) {
            try(var c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:"+port+"/?serverTimezone=Asia/Jakarta",user,pass)) {
                try(var s=c.createStatement()) {s.executeUpdate("CREATE DATABASE IF NOT EXISTS "+DB+" CHARACTER SET utf8mb4"); s.execute("USE "+DB);}
                // DDL is confined to the dedicated demo schema; never migrate existing schemas.
                try(var s=c.createStatement(); var r=s.executeQuery("SHOW TABLES")) {
                    if(r.next()) throw new IllegalStateException("Database demo sudah ada. Gunakan --verify; tidak menimpa atau menggandakan data.");
                }
                for(String sql:statements("Backend2/sql/schema.sql")) if(sql.startsWith("CREATE TABLE"))
                    try(var s=c.createStatement()){s.execute(sql);}
                c.setAutoCommit(false);
                try {
                    for(String sql:statements("Backend2/sql/seed.sql"))
                        if(sql.startsWith("INSERT INTO tantangan ") || sql.startsWith("INSERT INTO rekomendasi "))
                            try(var s=c.createStatement()){s.executeUpdate(sql);}
                    insert(c,"INSERT INTO pengguna (nama,email,password_hash,role,tanggal_lahir,golongan_usia,golongan_kesibukan,waktu_luang_menit,onboarding_selesai,created_at) VALUES (?,?,?,'user','1998-05-15','dewasa','luar_rumah',45,TRUE,?)",
                        "Demo Presentasi (Sintetis)",EMAIL,PasswordUtil.hashPassword(PASSWORD),START.atStartOfDay());
                    int id;
                    try(var s=c.createStatement();var r=s.executeQuery("SELECT id FROM pengguna")){r.next();id=r.getInt(1);}
                    var challenges = new ArrayList<int[]>();
                    try(var s=c.createStatement();var r=s.executeQuery("SELECT id,durasi_menit FROM tantangan WHERE id IN (1,2,5) ORDER BY id")){while(r.next()) challenges.add(new int[]{r.getInt(1),r.getInt(2)});}
                    if(challenges.size()!=3)throw new IllegalStateException("Katalog tantangan belum lengkap");
                    String[] activities={"Jalan kaki","Bersepeda santai","Peregangan","Jalan kaki","Senam ringan"};
                    String[] meals={"sarapan","makan_siang","makan_malam"};
                    String[][] menus={{"Oatmeal dan pisang","Roti telur dan pepaya","Bubur ayam"},{"Nasi, tempe, dan sayur bayam","Nasi, ikan, dan capcai","Sup ayam dan nasi"},{"Sup sayur dan tahu","Nasi, ayam, dan brokoli","Gado-gado dan telur"}};
                    for(int d=0;d<90;d++) {
                        LocalDate date=START.plusDays(d); var stamp=date.atStartOfDay(); int phase=d/30;
                        int minutes=10+phase*8+(d*7)%16;
                        insert(c,"INSERT INTO aktivitas (pengguna_id,tanggal,jenis_aktivitas,durasi_menit,keterangan,created_at) VALUES (?,?,?,?,?,?)",id,date,activities[d%5],minutes,"Data demo sintetis",stamp);
                        for(int h=0;h<4;h++)insert(c,"INSERT INTO hidrasi (pengguna_id,tanggal,jumlah_ml,catatan,created_at) VALUES (?,?,?,?,?)",id,date,300+phase*100+((d+h)%3)*50,"Data demo sintetis",stamp);
                        for(int m=0;m<3;m++)insert(c,"INSERT INTO kebiasaan_makan (pengguna_id,tanggal,waktu_makan,menu,keterangan,created_at) VALUES (?,?,?,?,?,?)",id,date,meals[m],menus[m][d%3],"Data demo sintetis",stamp);
                        int sleepMinutes=300+phase*45+(d*13)%91; LocalTime wake=LocalTime.of(6,0), bed=wake.minusMinutes(sleepMinutes);
                        insert(c,"INSERT INTO jadwal_tidur (pengguna_id,tanggal,jam_tidur,jam_bangun,durasi_jam,memenuhi_standar,pengingat_aktif,created_at) VALUES (?,?,?,?,?,?,FALSE,?)",id,date,Time.valueOf(bed),Time.valueOf(wake),Math.round(sleepMinutes/60.0*100)/100.0,sleepMinutes>=300,stamp);
                        for(int k=0;k<3;k++) {
                            boolean done=(d+k)% (phase+3)!=0;
                            String status=done?"selesai":(date.equals(END)?"disarankan":"dilewati");
                            insert(c,"INSERT INTO riwayat_tantangan (pengguna_id,tantangan_id,tanggal,durasi_target_menit,status,diselesaikan_pada,created_at) VALUES (?,?,?,?,?,?,?)",id,challenges.get(k)[0],date,challenges.get(k)[1],status,done?date.atTime(7,0):null,stamp);
                        }
                    }
                    c.commit();
                } catch(Exception e){c.rollback();throw e;}
            }
        }
        var session=new UserSession(); var profile=new AuthService(new PenggunaRepository(),session).login(EMAIL,PASSWORD);
        if(session.isAdmin() || !profile.isOnboardingSelesai())throw new IllegalStateException("Profil demo tidak valid");
        var access=new SessionAccess(session); var evaluation=new EvaluasiService(access);
        // Reuse actual owner-scoped calculations and writes; resume only missing weeks.
        if(args[0].equals("--execute")) for(int w=0;w<12;w++) {
            LocalDate end=END.minusDays(1+7*w);
            if(evaluation.getDaftarEvaluasi(profile.getId()).stream().noneMatch(e->e.getMingguMulai().equals(end.minusDays(6))))
                evaluation.buatEvaluasiMingguan(profile.getId(),end);
        }
        try(var c=DatabaseConfig.getConnection()) {
            for(String table:List.of("aktivitas","hidrasi","kebiasaan_makan","jadwal_tidur","riwayat_tantangan","evaluasi_mingguan"))
                try(var s=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE pengguna_id=?")) {
                    s.setInt(1,profile.getId());try(var r=s.executeQuery()){r.next();System.out.println(table+": "+r.getInt(1));}
                }
            try(var s=c.prepareStatement("SELECT MIN(tanggal),MAX(tanggal),COUNT(DISTINCT tanggal) FROM aktivitas WHERE pengguna_id=?")) {
                s.setInt(1,profile.getId());try(var r=s.executeQuery()){r.next();if(r.getInt(3)!=90)throw new IllegalStateException("Rentang bukan 90 hari");System.out.println("Riwayat tersimpan: "+r.getDate(1)+" s.d. "+r.getDate(2));}
            }
        }
        if(evaluation.getKonsistensi7HariTerakhir(profile.getId(),END).size()!=7)throw new IllegalStateException("Grafik belum terbaca");
        session.logout();System.out.println("Login AuthService dan pembacaan grafik berhasil.\nEmail: "+EMAIL+"\nPassword demo: "+PASSWORD);
    }
}
