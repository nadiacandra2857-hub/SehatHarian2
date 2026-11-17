package com.sehatharian.nutrition;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.HabitInsights;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Hard exclusions precede history ranking. Every review/meal commit reads current owner data again. */
public final class PersonalMenus extends MenuStore {
    public record Candidate(int id,String key,int revision,String title,String description,MenuComposition.Mapped mapping,String reason,int recentCount){}
    public record Result(String state,String reason,String fingerprint,List<Candidate> items,int unassessable,int excluded){public Result{items=List.copyOf(items);}}
    public record Review(int owner,int menuId,LocalDate day,String mealTime,double portions,String token,String title,Nutrients nutrients,String details,String source,int revision,double recipeServings,long preferenceRevision){}
    public static final class Changed extends IllegalStateException{public Changed(){super("Menu, bahan, preferensi atau catatan berubah sejak tinjauan. Tinjau ulang sebelum mencatat porsi.");}}
    public PersonalMenus(SessionAccess access){super(access);}
    public PersonalMenus(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public Result recommend(LocalDate end)throws SQLException{return transaction(true,(c,tx)->evaluate(c,end));}
    public List<Candidate> catalog()throws SQLException{return transaction(true,(c,tx)->candidates(c));}
    private List<Candidate> candidates(Connection c)throws SQLException{
        List<Candidate> out=new ArrayList<>();try(var p=c.prepareStatement("SELECT id,content_key,revision,title,body FROM content_version WHERE kind='MENU' AND state='PUBLISHED' ORDER BY id" );var r=p.executeQuery()){
            while(r.next())out.add(new Candidate(r.getInt(1),r.getString(2),r.getInt(3),r.getString(4),r.getString(5),MenuComposition.load(c,r.getInt(1)),"Katalog umum · kecocokan pribadi belum dinilai",0));
        }return List.copyOf(out);
    }
    private Result evaluate(Connection c,LocalDate end)throws SQLException{
        var profile=MenuPreferences.load(c,access);var gate=new HabitInsights(access).nutritionGate(c,end);
        List<Candidate> published=candidates(c);List<String> signature=new ArrayList<>(List.of(Integer.toString(access.owner()),profile.signature(),gate.signature()));
        for(var item:published){signature.add(Integer.toString(item.id()));signature.add(item.mapping().currentHash());signature.add(Objects.toString(item.mapping().reviewHash(),"unreviewed"));}
        String fingerprint=digest(signature);
        if(!gate.state().equals("READY"))return new Result(gate.state(),gate.reason(),fingerprint,List.of(),0,0);
        if(profile.gate()!=null)return new Result("UNASSESSABLE",profile.gate(),fingerprint,List.of(),0,0);
        List<Candidate> eligible=new ArrayList<>();int unknown=0,excluded=0;
        for(var item:published){
            var mapped=item.mapping();boolean assessable=mapped.eligible();
            try{MenuComposition.validateComplete(mapped);}catch(IllegalArgumentException invalid){assessable=false;}
            if(!assessable){unknown++;continue;}
            boolean conflict=false;
            for(var line:mapped.composition().lines()){
                var ingredient=mapped.ingredients().get(line.ingredientId());
                if(profile.restrictions().contains(ingredient.id())||profile.avoid().contains(ingredient.id())||!Collections.disjoint(profile.allergens(),ingredient.allergens())){conflict=true;break;}
            }
            if(!conflict)for(var ingredient:mapped.ingredients().values())if(!dietAllows(profile.diet(),ingredient.origin())){conflict=true;break;}
            if(conflict){excluded++;continue;}
            String reason=switch(profile.diet()){case VEGETARIAN->"Sesuai pilihan vegetarian dan bahan yang tercatat pada menu ini.";case VEGAN->"Sesuai pilihan vegan dan bahan yang tercatat pada menu ini.";default->"Sesuai pilihan pola makan dan penyaringan bahan yang Anda simpan.";};
            int count=gate.menuFrequency().getOrDefault(item.key(),0);
            eligible.add(new Candidate(item.id(),item.key(),item.revision(),item.title(),item.description(),mapped,reason,count));
        }
        eligible.sort(Comparator.comparingInt(Candidate::recentCount).thenComparing(Candidate::title).thenComparingInt(Candidate::id));
        String state=!eligible.isEmpty()?"READY":unknown>0&&excluded==0?"UNASSESSABLE":"EMPTY";
        String reason=gate.reason()+" "+eligible.size()+" menu cocok; "+excluded+" disaring, "+unknown+" belum dapat dinilai. "+(eligible.isEmpty()?"Pantangan tetap diterapkan; jelajahi katalog umum atau lengkapi bahan/preferensi.":"Kecocokan berdasarkan bahan tersimpan, bukan jaminan bebas alergen.");
        return new Result(state,reason,fingerprint,eligible,unknown,excluded);
    }
    public Review review(int menu,LocalDate day,String time,double portions)throws SQLException{return transaction(true,(c,tx)->review(c,menu,day,time,portions));}
    private Review review(Connection c,int menu,LocalDate day,String time,double portions)throws SQLException{
        if(day==null||!Set.of("sarapan","makan_siang","makan_malam","camilan").contains(time)||!Double.isFinite(portions)||portions<=0||portions>100)throw new IllegalArgumentException("Tanggal, waktu makan dan porsi 0–100 wajib valid.");
        var result=evaluate(c,day);var item=result.items().stream().filter(v->v.id()==menu).findFirst().orElseThrow(()->new IllegalStateException("Menu belum dapat dicocokkan dengan data Anda saat ini. "+result.reason()));
        var mapping=item.mapping();var composition=mapping.composition();var profile=MenuPreferences.load(c,access);
        List<String> tokenParts=new ArrayList<>(List.of(result.fingerprint(),Integer.toString(menu),day.toString(),time,Double.toString(portions)));
        List<Map<String,Double>> components=new ArrayList<>();StringBuilder details=new StringBuilder("Versi pemetaan: "+mapping.currentHash()+"\nPorsi resep: ").append(portions).append(" dari hasil ").append(composition.servings()).append(" porsi. Estimasi dari bahan; bukan bobot makanan jadi.\n");
        for(var line:composition.lines()){
            var ingredient=mapping.ingredients().get(line.ingredientId());details.append(ingredient.name()).append(" [").append(ingredient.id()).append(" v").append(ingredient.revision()).append("; ").append(ingredient.origin()).append("; kategori tercatat ").append(ingredient.allergens()).append("] · ").append(line.quantity()==null?"jumlah belum diketahui":line.quantity()+" "+line.unit()).append(" · sumber bahan: ").append(ingredient.source()).append(" · dasar: ").append(ingredient.basis()).append('\n');
            Double grams=line.grams();Map<String,Double> values=new HashMap<>();
            if(line.foodId()!=null){try(var p=c.prepareStatement("SELECT nutrients,source,version,name FROM food_catalog WHERE food_id=? FOR UPDATE")){p.setString(1,line.foodId());try(var r=p.executeQuery()){
                if(r.next()){
                    Collections.addAll(tokenParts,line.foodId(),r.getString(1),r.getString(2),r.getString(3),r.getString(4));
                    details.append("Katalog: ").append(line.foodId()).append(" · ").append(r.getString(4)).append(" · ").append(r.getString(2)).append(" · versi ").append(r.getString(3)).append('\n');
                    if(grams!=null&&line.nutritionVerified()&&Objects.equals(line.foodVersion(),r.getString(3))){double factor=grams/100*portions/composition.servings();Nutrients.decode(r.getString(1)).values().forEach((key,value)->values.put(key,value*factor));}
                }
            }}}
            if(grams==null)details.append("Konversi ke gram belum diketahui; nilai gizi tidak dianggap lengkap.\n");
            if(!line.unit().equals("g"))details.append("Sumber konversi: ").append(line.conversionSource().isBlank()?"belum diketahui":line.conversionSource()).append('\n');
            details.append("Dasar satuan gizi: ").append(line.nutritionVerified()?line.basisSource():"belum diverifikasi").append('\n');components.add(values);
        }
        Map<String,Double> totals=new HashMap<>();for(String key:Nutrients.KEYS)if(!components.isEmpty()&&components.stream().allMatch(v->v.containsKey(key)))totals.put(key,components.stream().mapToDouble(v->v.get(key)).sum());
        var nutrients=new Nutrients(totals);details.append(totals.size()==Nutrients.KEYS.size()?"Estimasi gizi seluruh parameter tersedia.":"Estimasi gizi belum lengkap; parameter tanpa dukungan seluruh bahan tetap belum diketahui.").append('\n').append(LIMIT);
        String source=composition.source()+" | "+ALLERGEN_SOURCE+" | "+DIET_SOURCE;
        return new Review(access.owner(),menu,day,time,portions,digest(tokenParts),item.title(),nutrients,details.toString(),source,item.revision(),composition.servings(),profile.revision());
    }
    public int log(Review expected)throws SQLException{
        access.owner(expected.owner());
        return transaction(true,(c,tx)->{
            Review current;try{current=review(c,expected.menuId(),expected.day(),expected.mealTime(),expected.portions());}catch(IllegalStateException invalid){throw new Changed();}
            if(!current.token().equals(expected.token()))throw new Changed();
            String key;try(var p=c.prepareStatement("SELECT content_key FROM content_version WHERE id=?")){p.setInt(1,current.menuId());try(var r=p.executeQuery()){r.next();key=r.getString(1);}}
            int meal;try(var p=c.prepareStatement("INSERT INTO kebiasaan_makan(pengguna_id,tanggal,waktu_makan,menu,keterangan) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,access.owner());p.setDate(2,java.sql.Date.valueOf(current.day()));p.setString(3,current.mealTime());p.setString(4,current.title());p.setString(5,current.portions()+" porsi resep · menu v"+current.revision()+" · snapshot bahan dan estimasi gizi tersimpan");if(tx.execute(p)!=1)throw new SQLException();try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException();meal=r.getInt(1);}
            }
            try(var p=c.prepareStatement("INSERT INTO menu_meal_snapshot(meal_id,pengguna_id,menu_id,content_key,content_revision,preference_revision,portions,recipe_servings,nutrients,source,details,review_token) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")){
                p.setInt(1,meal);p.setInt(2,access.owner());p.setInt(3,current.menuId());p.setString(4,key);p.setInt(5,current.revision());p.setLong(6,current.preferenceRevision());p.setDouble(7,current.portions());p.setDouble(8,current.recipeServings());p.setString(9,current.nutrients().encode());p.setString(10,current.source());p.setString(11,current.details());p.setString(12,current.token());if(tx.execute(p)!=1)throw new SQLException();
            }return meal;
        });
    }
}
