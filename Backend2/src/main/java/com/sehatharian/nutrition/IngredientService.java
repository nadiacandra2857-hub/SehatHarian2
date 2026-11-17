package com.sehatharian.nutrition;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.PenggunaRepository;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class IngredientService extends MenuStore {
    public IngredientService(SessionAccess access){super(access);}
    public IngredientService(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public List<Ingredient> search(String query)throws SQLException{
        if(query==null||query.length()>160)throw new IllegalArgumentException("Pencarian maksimal 160 karakter.");
        try(var c=open();var p=c.prepareStatement("SELECT DISTINCT i.ingredient_id FROM ingredient_definition i LEFT JOIN ingredient_alias a ON a.ingredient_id=i.ingredient_id WHERE i.name_id LIKE ? OR a.alias_key LIKE ? ORDER BY i.name_id LIMIT 50")){
            p.setString(1,"%"+query.strip()+"%");p.setString(2,"%"+normalize(query)+"%");List<Ingredient> out=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())out.add(get(c,r.getString(1)));}return access.result(List.copyOf(out));
        }
    }
    public Ingredient get(String id)throws SQLException{try(var c=open()){return access.result(get(c,id));}}
    public static Ingredient get(Connection c,String id)throws SQLException{
        try(var p=c.prepareStatement("SELECT * FROM ingredient_definition WHERE ingredient_id=?")){
            p.setString(1,id);try(var r=p.executeQuery()){
                if(!r.next())throw new IllegalArgumentException("Bahan belum terdaftar: "+id);
                List<String> aliases=new ArrayList<>();try(var a=c.prepareStatement("SELECT alias_key FROM ingredient_alias WHERE ingredient_id=? ORDER BY alias_key")){a.setString(1,id);try(var ar=a.executeQuery()){while(ar.next())aliases.add(ar.getString(1));}}
                return new Ingredient(id,r.getString("name_id"),Origin.valueOf(r.getString("origin")),Assessment.valueOf(r.getString("assessment")),allergens(r.getString("allergens")),r.getString("source"),r.getString("review_basis"),r.getLong("revision"),aliases);
            }
        }
    }
    public static String resolve(Connection c,String alias)throws SQLException{
        try(var p=c.prepareStatement("SELECT ingredient_id FROM ingredient_alias WHERE alias_key=?")){p.setString(1,normalize(alias));try(var r=p.executeQuery()){return r.next()?r.getString(1):null;}}
    }
    public String save(String id,String name,Origin origin,Assessment assessment,Set<Allergen> allergens,List<String> aliases,String source,String basis)throws SQLException{
        return save(id,name,origin,assessment,allergens,aliases,source,basis,false);
    }
    /** Import may add missing definitions or reuse exact content; it cannot overwrite editorial changes. */
    public String importDefinition(Ingredient expected)throws SQLException {
        return save(expected.id(),expected.name(),expected.origin(),expected.assessment(),expected.allergens(),expected.aliases(),expected.source(),expected.basis(),true);
    }
    public static boolean sameDefinition(Ingredient actual,Ingredient expected){
        Set<String> aliases=new TreeSet<>();aliases.add(normalize(expected.id()));aliases.add(normalize(expected.name()));expected.aliases().forEach(v->aliases.add(normalize(v)));
        return actual.id().equals(expected.id())&&actual.name().equals(expected.name().strip())&&actual.origin()==expected.origin()&&actual.assessment()==expected.assessment()&&actual.allergens().equals(expected.allergens())&&actual.source().equals(expected.source().strip())&&actual.basis().equals(expected.basis().strip())&&new TreeSet<>(actual.aliases()).equals(aliases);
    }
    private String save(String id,String name,Origin origin,Assessment assessment,Set<Allergen> allergens,List<String> aliases,String source,String basis,boolean importOnly)throws SQLException {
        access.requireAdmin();
        if(id==null||!id.matches("[a-z0-9][a-z0-9._:-]{0,79}")||name==null||name.isBlank()||name.length()>150||source==null||basis==null||source.length()>8000||basis.length()>8000||origin==null||assessment==null||aliases.size()>30)throw new IllegalArgumentException("ID stabil, nama dan data peninjauan bahan wajib valid.");
        if(assessment==Assessment.REVIEWED&&(origin==Origin.UNKNOWN||source.isBlank()||basis.isBlank()))throw new IllegalArgumentException("Bahan ditinjau memerlukan asal, sumber dan dasar peninjauan.");
        Set<String> keys=new TreeSet<>();keys.add(normalize(name));keys.add(normalize(id));for(String value:aliases){String key=normalize(value);if(key.isBlank()||key.length()>160)throw new IllegalArgumentException("Alias tidak valid.");keys.add(key);}
        return transaction(false,(c,tx)->{
            access.requireAdmin();
            if(importOnly)try(var p=c.prepareStatement("SELECT ingredient_id FROM ingredient_definition WHERE ingredient_id=?")){p.setString(1,id);try(var r=p.executeQuery()){if(r.next()){
                var expected=new Ingredient(id,name,origin,assessment,allergens,source,basis,0,aliases);
                if(!sameDefinition(get(c,id),expected))throw new IllegalArgumentException("Definisi telah diubah admin; impor tidak menimpa: "+id);
                return id;
            }}}
            for(String key:keys){String existing=resolve(c,key);if(existing!=null&&!existing.equals(id))throw new IllegalArgumentException("Alias sudah dipakai bahan lain: "+key);}
            try(var p=c.prepareStatement("INSERT INTO ingredient_definition(ingredient_id,name_id,origin,assessment,allergens,source,review_basis,revision,reviewed_by,updated_at) VALUES(?,?,?,?,?,?,?,1,?,?) ON DUPLICATE KEY UPDATE name_id=VALUES(name_id),origin=VALUES(origin),assessment=VALUES(assessment),allergens=VALUES(allergens),source=VALUES(source),review_basis=VALUES(review_basis),revision=revision+1,reviewed_by=VALUES(reviewed_by),updated_at=VALUES(updated_at)")){
                p.setString(1,id);p.setString(2,name.strip());p.setString(3,origin.name());p.setString(4,assessment.name());p.setString(5,MenuModels.allergens(allergens));p.setString(6,source.strip());p.setString(7,basis.strip());p.setInt(8,access.requireAdmin());p.setObject(9,LocalDateTime.ofInstant(Instant.now(),ZoneOffset.UTC));tx.execute(p);
            }
            try(var p=c.prepareStatement("DELETE FROM ingredient_alias WHERE ingredient_id=?")){p.setString(1,id);tx.execute(p);}
            for(String key:keys)try(var p=c.prepareStatement("INSERT INTO ingredient_alias(alias_key,ingredient_id) VALUES(?,?)")){p.setString(1,key);p.setString(2,id);tx.execute(p);}
            return id;
        });
    }
}
