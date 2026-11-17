package com.sehatharian.service;
import com.sehatharian.repository.*;
import com.sehatharian.model.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Saved-data gate shared by recommendations, challenges and weekly review. No score. */
public final class HabitInsights extends OwnedRepository {
    public static final Map<String,String> UNITS=Map.of("activity","menit","hydration","ml","nutrition","catatan makan","sleep","jam aktual");
    public record Advice(String state,String reason,List<Rekomendasi> items) {}
    public HabitInsights(SessionAccess access){super(access);}
    public Map<LocalDate,Map<String,Double>> days(LocalDate end,int count)throws SQLException {
        if(end==null||count<1||count>90)throw new IllegalArgumentException("Periode harus 1–90 hari.");
        Map<LocalDate,Map<String,Double>> result=new LinkedHashMap<>();LocalDate start=end.minusDays(count-1);
        for(int i=0;i<count;i++)result.put(start.plusDays(i),new HashMap<>());
        String[][] queries={
            {"activity","SELECT tanggal,SUM(durasi_menit) FROM aktivitas WHERE pengguna_id=? AND tanggal BETWEEN ? AND ? GROUP BY tanggal"},
            {"hydration","SELECT tanggal,SUM(jumlah_ml) FROM hidrasi WHERE pengguna_id=? AND tanggal BETWEEN ? AND ? GROUP BY tanggal"},
            {"nutrition","SELECT tanggal,COUNT(*) FROM kebiasaan_makan WHERE pengguna_id=? AND tanggal BETWEEN ? AND ? GROUP BY tanggal"},
            {"sleep","SELECT DATE(ended_at),SUM(TIMESTAMPDIFF(SECOND,started_at,ended_at))/3600.0 FROM sleep_actual WHERE pengguna_id=? AND DATE(ended_at) BETWEEN ? AND ? GROUP BY DATE(ended_at)"}};
        try(var c=open()){for(var q:queries)try(var p=c.prepareStatement(q[1])){p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(start));p.setDate(3,java.sql.Date.valueOf(end));try(var r=p.executeQuery()){while(r.next())result.get(r.getDate(1).toLocalDate()).put(q[0],r.getDouble(2));}}}
        return access.result(result);
    }
    public record NutritionGate(String state,String reason,String signature,Map<String,Integer> menuFrequency){}
    /** Same 7-day/3-recorded-day rule, read from this owned transaction before personal menu matching. */
    public NutritionGate nutritionGate(Connection c,LocalDate end)throws SQLException {
        if(end==null)throw new IllegalArgumentException("Tanggal penilaian wajib.");
        List<String> signature=new ArrayList<>(List.of(end.toString()));boolean profileValid;
        try(var p=c.prepareStatement("SELECT onboarding_selesai,waktu_luang_menit,golongan_usia,golongan_kesibukan,nama,tanggal_lahir FROM pengguna WHERE id=?")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){
                if(!r.next())throw new SecurityException("Profil tidak tersedia.");
                profileValid=r.getBoolean(1)&&r.getInt(2)>0&&Set.of("dewasa","lansia").contains(r.getString(3))&&Set.of("dalam_rumah","luar_rumah").contains(r.getString(4));
                for(int i=1;i<=6;i++)signature.add(Objects.toString(r.getObject(i),"unknown"));
            }
        }
        Set<LocalDate> dates=new HashSet<>();Map<String,Integer> frequency=new HashMap<>();
        try(var p=c.prepareStatement("SELECT m.id,m.tanggal,m.waktu_makan,m.menu,m.keterangan,s.content_key FROM kebiasaan_makan m LEFT JOIN menu_meal_snapshot s ON s.meal_id=m.id AND s.pengguna_id=m.pengguna_id WHERE m.pengguna_id=? AND m.tanggal BETWEEN ? AND ? ORDER BY m.id FOR UPDATE")){
            p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(end.minusDays(6)));p.setDate(3,java.sql.Date.valueOf(end));
            try(var r=p.executeQuery()){while(r.next()){dates.add(r.getDate(2).toLocalDate());for(int i=1;i<=6;i++)signature.add(Objects.toString(r.getObject(i),"unknown"));if(r.getString(6)!=null)frequency.merge(r.getString(6),1,Integer::sum);}}
        }
        String state=!profileValid?"INSUFFICIENT":dates.isEmpty()?"EMPTY":dates.size()<3?"INSUFFICIENT":"READY";
        String reason=!profileValid?"Lengkapi profil, kelompok usia, rutinitas dan waktu luang tersimpan.":dates.size()+"/7 hari makan tercatat pada "+end.minusDays(6)+"–"+end+". "+(dates.size()<3?"Aturan proyek memerlukan minimal 3 hari catatan.":"Urutan menu mempertimbangkan variasi menu yang dicatat dalam periode ini.");
        return new NutritionGate(state,reason,com.sehatharian.nutrition.MenuModels.digest(signature),Map.copyOf(frequency));
    }
    /** Sleep gate on the caller's owned transaction, so source fingerprint and advice agree. */
    public Advice sleepAdvice(Connection c,LocalDate day)throws SQLException {
        boolean complete;
        try(var p=c.prepareStatement("SELECT onboarding_selesai,waktu_luang_menit,golongan_usia,golongan_kesibukan FROM pengguna WHERE id=?")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){complete=r.next()&&r.getBoolean(1)&&r.getInt(2)>0&&Set.of("dewasa","lansia").contains(r.getString(3))&&Set.of("dalam_rumah","luar_rumah").contains(r.getString(4));}}
        // Read relevant saved preferences, even though food restrictions are not a sleep intervention.
        try(var p=c.prepareStatement("SELECT hydration_ml,diet,avoid_foods FROM habit_preferences WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){while(r.next()){r.getObject(1);r.getString(2);r.getString(3);}}}
        int days=0;double total=0;
        try(var p=c.prepareStatement("SELECT DATE(ended_at),SUM(TIMESTAMPDIFF(SECOND,started_at,ended_at)) FROM sleep_actual WHERE pengguna_id=? AND ended_at>=? AND ended_at<? GROUP BY DATE(ended_at)")){
            p.setInt(1,access.owner());p.setObject(2,day.minusDays(6).atStartOfDay());p.setObject(3,day.plusDays(1).atStartOfDay());try(var r=p.executeQuery()){while(r.next()){days++;total+=r.getLong(2);}}}
        String period=day.minusDays(6)+"–"+day;
        if(!complete)return new Advice("INSUFFICIENT","Lengkapi profil, kelompok usia, rutinitas dan waktu luang tersimpan. Periode "+period+".",List.of());
        if(days<3)return new Advice(days==0?"EMPTY":"INSUFFICIENT",days+"/7 hari tercatat pada "+period+"; minimal 3 hari diperlukan untuk saran. Hari kosong belum diketahui.",List.of());
        return new Advice("READY",String.format(Locale.ROOT,"%d/7 hari tercatat pada %s; rata-rata %.2f jam pada hari tercatat. Tinjau kesempatan istirahat dan jadwal pilihan Anda; kualitas tidur tidak diukur.",days,period,total/days/3600),List.of());
    }
    public Advice advise(String category,LocalDate end)throws SQLException {
        if(!UNITS.containsKey(category))throw new IllegalArgumentException();
        if(category.equals("nutrition")){var result=new com.sehatharian.nutrition.PersonalMenus(access,connections).recommend(end);return new Advice(result.state(),result.reason(),List.of());}
        Pengguna p=new PenggunaRepository().getById(access.owner(),access);
        if(p==null||!p.isOnboardingSelesai()||p.getWaktuLuangMenit()<=0||!Set.of("dewasa","lansia").contains(p.getGolonganUsia())||!Set.of("dalam_rumah","luar_rumah").contains(p.getGolonganKesibukan()))
            return new Advice("INSUFFICIENT","Lengkapi profil, kelompok usia, rutinitas, dan waktu luang tersimpan.",List.of());
        var pref=new HabitPreferences(access).get();
        if(category.equals("nutrition")&&(pref.diet()==null||pref.diet().isBlank()||pref.avoid()==null||pref.avoid().isBlank()))return new Advice("INSUFFICIENT","Isi pola makan dan pantangan (tulis tidak ada bila tidak memiliki) di Progres.",List.of());
        var data=days(end,7);long recorded=data.values().stream().filter(v->v.containsKey(category)).count();
        if(recorded==0)return new Advice("EMPTY","Belum tercatat dalam periode "+end.minusDays(6)+"–"+end+". Mulai catat kebiasaan ini.",List.of());
        if(recorded<3)return new Advice("INSUFFICIENT",recorded+"/7 hari tercatat; aturan proyek memerlukan minimal 3 hari per kebiasaan untuk saran. Lengkapi catatan.",List.of());
        double avg=data.values().stream().filter(v->v.containsKey(category)).mapToDouble(v->v.get(category)).average().orElseThrow();
        String reason=String.format(Locale.ROOT,"%d/7 hari tercatat, rata-rata %.1f %s pada hari tercatat (%s–%s). Profil tersimpan: %s, %s, waktu luang %d menit.",recorded,avg,UNITS.get(category),end.minusDays(6),end,p.getGolonganUsia(),p.getGolonganKesibukan(),p.getWaktuLuangMenit());
        List<Rekomendasi> items=List.of();
        if(category.equals("activity"))items=new RekomendasiService().getAktivitasSesuaiWaktuLuang(p.getWaktuLuangMenit(),p.getGolonganKesibukan(),p.getGolonganUsia());
        // Food composition has no verified ingredient/allergen mapping. Never infer safe menu suggestions.
        return access.result(new Advice("READY",reason+switch(category){case "sleep"->avg<5?" Durasi di bawah ambang internal 5 jam. Tinjau jadwal tidur.":"Pertahankan pencatatan tidur aktual.";case "hydration"->pref.hydration()==null?" Atur target pilihan di Hidrasi.":" Bandingkan dengan target pilihan Anda "+pref.hydration()+" ml; bukan kebutuhan medis.";case "nutrition"->" Tinjau variasi catatan; rekomendasi menu personal ditahan karena pemetaan bahan/pantangan belum tersedia.";default->" Pilih satu aktivitas dalam waktu luang; durasi kandidat bukan durasi total program.";},items));
    }
}
