package com.sehatharian.nutrition;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.PenggunaRepository;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.util.*;

public final class MenuPreferences extends MenuStore {
    public record Profile(long revision,Diet diet,Declaration avoidStatus,Declaration restrictionStatus,Set<String> avoid,Set<String> restrictions,Set<Allergen> allergens,String rawAvoid,String rawRestrictions,List<String> unresolved){
        public Profile{avoid=Set.copyOf(avoid);restrictions=Set.copyOf(restrictions);allergens=Set.copyOf(allergens);unresolved=List.copyOf(unresolved);}
        public String gate(){
            if(diet==Diet.UNKNOWN||avoidStatus==Declaration.UNKNOWN||restrictionStatus==Declaration.UNKNOWN)return "Lengkapi pola makan, pilihan bahan yang dihindari dan pernyataan pantangan tersimpan. Pilihan kosong berarti belum diketahui.";
            if(!unresolved.isEmpty())return "Belum dapat dicocokkan: teks belum terpetakan — "+String.join(", ",unresolved)+". Pilih bahan terstruktur atau perjelas input tersebut.";
            return null;
        }
        public String signature(){List<String> parts=new ArrayList<>(List.of(Long.toString(revision),diet.name(),avoidStatus.name(),restrictionStatus.name(),MenuModels.allergens(allergens),rawAvoid,rawRestrictions));parts.addAll(avoid.stream().sorted().map(s->"avoid:"+s).toList());parts.addAll(restrictions.stream().sorted().map(s->"restriction:"+s).toList());parts.addAll(unresolved);return digest(parts);}
    }
    public MenuPreferences(SessionAccess access){super(access);}
    public MenuPreferences(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public Profile get()throws SQLException{try(var c=open()){return access.result(load(c,access));}}
    public static Profile load(Connection c,SessionAccess access)throws SQLException{
        long revision=0;Diet diet=Diet.UNKNOWN;Declaration avoid=Declaration.UNKNOWN,restrictions=Declaration.UNKNOWN;Set<Allergen> allergens=Set.of();String rawAvoid="",rawRestrictions="";List<String> unresolved=List.of();
        try(var p=c.prepareStatement("SELECT * FROM menu_preferences WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){if(r.next()){revision=r.getLong("revision");diet=Diet.valueOf(r.getString("diet"));avoid=Declaration.valueOf(r.getString("avoid_status"));restrictions=Declaration.valueOf(r.getString("restriction_status"));allergens=MenuModels.allergens(r.getString("allergens"));rawAvoid=r.getString("raw_avoid");rawRestrictions=r.getString("raw_restrictions");unresolved=r.getString("unresolved").isBlank()?List.of():List.of(r.getString("unresolved").split("\n"));}}}
        if(revision==0)try(var p=c.prepareStatement("SELECT avoid_foods FROM habit_preferences WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){if(r.next())rawRestrictions=Objects.toString(r.getString(1),"");}}
        Set<String> avoided=new TreeSet<>(),reported=new TreeSet<>();try(var p=c.prepareStatement("SELECT ingredient_id,reason FROM menu_preference_ingredient WHERE pengguna_id=?")){p.setInt(1,access.owner());try(var r=p.executeQuery()){while(r.next())(r.getString(2).equals("PREFERENCE")?avoided:reported).add(r.getString(1));}}
        return new Profile(revision,diet,avoid,restrictions,avoided,reported,allergens,rawAvoid,rawRestrictions,unresolved);
    }
    public Profile save(Diet diet,Declaration avoidStatus,Declaration restrictionStatus,Set<String> avoided,Set<String> restrictions,Set<Allergen> allergens,String rawAvoid,String rawRestrictions)throws SQLException{
        access.owner();Objects.requireNonNull(diet);Objects.requireNonNull(avoidStatus);Objects.requireNonNull(restrictionStatus);
        if(rawAvoid==null||rawRestrictions==null||rawAvoid.length()>2000||rawRestrictions.length()>2000||avoided.size()>50||restrictions.size()>50)throw new IllegalArgumentException("Preferensi maksimal 50 bahan dan teks 2.000 karakter.");
        if(avoidStatus==Declaration.NONE&&(!avoided.isEmpty()||!rawAvoid.isBlank())||restrictionStatus==Declaration.NONE&&(!restrictions.isEmpty()||!allergens.isEmpty()||!rawRestrictions.isBlank()))throw new IllegalArgumentException("Pernyataan tidak ada harus tanpa pilihan atau teks yang bertentangan.");
        if(avoidStatus==Declaration.SELECTED&&avoided.isEmpty()&&rawAvoid.isBlank()||restrictionStatus==Declaration.SELECTED&&restrictions.isEmpty()&&allergens.isEmpty()&&rawRestrictions.isBlank())throw new IllegalArgumentException("Pilih atau tulis bahan/pantangan yang dilaporkan.");
        return transaction(true,(c,tx)->{
            Set<String> avoid=new TreeSet<>(avoided),reported=new TreeSet<>(restrictions);List<String> unresolved=new ArrayList<>();
            resolve(c,rawAvoid,avoid,unresolved,"Preferensi");resolve(c,rawRestrictions,reported,unresolved,"Pantangan");
            for(String id:avoid)IngredientService.get(c,id);for(String id:reported)IngredientService.get(c,id);
            try(var p=c.prepareStatement("INSERT INTO menu_preferences(pengguna_id,revision,diet,avoid_status,restriction_status,allergens,raw_avoid,raw_restrictions,unresolved) VALUES(?,1,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE revision=revision+1,diet=VALUES(diet),avoid_status=VALUES(avoid_status),restriction_status=VALUES(restriction_status),allergens=VALUES(allergens),raw_avoid=VALUES(raw_avoid),raw_restrictions=VALUES(raw_restrictions),unresolved=VALUES(unresolved)")){
                p.setInt(1,access.owner());p.setString(2,diet.name());p.setString(3,avoidStatus.name());p.setString(4,restrictionStatus.name());p.setString(5,MenuModels.allergens(allergens));p.setString(6,rawAvoid);p.setString(7,rawRestrictions);p.setString(8,String.join("\n",unresolved));tx.execute(p);
            }
            try(var p=c.prepareStatement("DELETE FROM menu_preference_ingredient WHERE pengguna_id=?")){p.setInt(1,access.owner());tx.execute(p);}
            for(var entry:Map.of("PREFERENCE",avoid,"RESTRICTION",reported).entrySet())for(String id:entry.getValue())try(var p=c.prepareStatement("INSERT INTO menu_preference_ingredient(pengguna_id,ingredient_id,reason) VALUES(?,?,?)")){p.setInt(1,access.owner());p.setString(2,id);p.setString(3,entry.getKey());tx.execute(p);}
            return load(c,access);
        });
    }
    private static void resolve(Connection c,String raw,Set<String> values,List<String> unresolved,String label)throws SQLException{
        for(String token:raw.split("[,;\\n]")){if(token.isBlank())continue;String id=IngredientService.resolve(c,token);if(id==null)unresolved.add(label+": "+token.strip());else values.add(id);}
    }
    public static void invalidateLegacy(Connection c,com.sehatharian.storage.WriteTransaction tx,SessionAccess access)throws SQLException{
        try(var p=c.prepareStatement("UPDATE menu_preferences SET revision=revision+1,diet='UNKNOWN',restriction_status='UNKNOWN' WHERE pengguna_id=?")){p.setInt(1,access.owner());tx.execute(p);}
    }
}
