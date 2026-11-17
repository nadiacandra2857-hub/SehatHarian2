package com.sehatharian.nutrition;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.storage.WriteTransaction;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.util.*;

/** Mapping belongs to an immutable content_version. Drafting a revision never changes a published mapping. */
public final class MenuComposition extends MenuStore {
    public record Mapped(Composition composition,String reviewHash,String currentHash,Map<String,Ingredient> ingredients,String image,String caption){
        public boolean eligible(){return composition.completeness()==Completeness.COMPLETE&&reviewHash!=null&&reviewHash.equals(currentHash);}
        public String label(){return eligible()?"Bahan sudah ditinjau":composition.completeness()==Completeness.INCOMPLETE?"Bahan belum lengkap":composition.completeness()==Completeness.COMPLETE?"Belum dapat dicocokkan · perlu peninjauan ulang":"Bahan belum ditinjau / belum lengkap";}
    }
    public MenuComposition(SessionAccess access){super(access);}
    public MenuComposition(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public Mapped admin(int menu)throws SQLException{access.requireAdmin();return transaction(false,(c,tx)->{requireMenu(c,menu,false);return load(c,menu);});}
    public Mapped published(int menu)throws SQLException{return transaction(true,(c,tx)->{requireMenu(c,menu,true);return load(c,menu);});}
    public static void requireMenu(Connection c,int id,boolean published)throws SQLException{
        try(var p=c.prepareStatement("SELECT kind,state FROM content_version WHERE id=?")){p.setInt(1,id);try(var r=p.executeQuery()){if(!r.next()||!r.getString(1).equals("MENU")||published&&!r.getString(2).equals("PUBLISHED"))throw new IllegalArgumentException("Versi menu tidak diterbitkan.");}}
    }
    public static Mapped load(Connection c,int menu)throws SQLException{
        Completeness completeness=Completeness.UNREVIEWED;Double servings=null;String source="",review=null;
        try(var p=c.prepareStatement("SELECT * FROM menu_composition WHERE menu_id=?")){p.setInt(1,menu);try(var r=p.executeQuery()){if(r.next()){completeness=Completeness.valueOf(r.getString("completeness"));servings=r.getObject("recipe_servings")==null?null:r.getDouble("recipe_servings");source=r.getString("source");review=r.getString("review_hash");}}}
        List<Line> lines=new ArrayList<>();Map<String,Ingredient> ingredients=new LinkedHashMap<>();
        try(var p=c.prepareStatement("SELECT * FROM menu_ingredient WHERE menu_id=? ORDER BY position")){p.setInt(1,menu);try(var r=p.executeQuery()){while(r.next()){
            String id=r.getString("ingredient_id");if(id!=null)ingredients.put(id,IngredientService.get(c,id));
            lines.add(new Line(id,r.getString("unresolved_name"),r.getObject("quantity")==null?null:r.getDouble("quantity"),r.getString("unit"),r.getObject("grams_per_unit")==null?null:r.getDouble("grams_per_unit"),r.getString("conversion_source"),r.getString("food_id"),r.getString("food_version"),r.getBoolean("nutrition_verified"),r.getString("basis_source")));
        }}}
        var composition=new Composition(completeness,servings,source,lines);
        List<String> parts=new ArrayList<>(List.of(completeness.name(),Objects.toString(servings,"unknown"),source));
        for(var line:lines){Collections.addAll(parts,line.ingredientId(),line.unresolvedName(),Objects.toString(line.quantity(),null),line.unit(),Objects.toString(line.gramsPerUnit(),null),line.conversionSource(),line.foodId(),line.foodVersion(),Boolean.toString(line.nutritionVerified()),line.basisSource());if(line.ingredientId()!=null){var i=ingredients.get(line.ingredientId());Collections.addAll(parts,i.id(),i.name(),i.origin().name(),i.assessment().name(),MenuModels.allergens(i.allergens()),i.source(),i.basis(),Long.toString(i.revision()));}}
        String image="nutrition",caption="Ilustrasi kategori makanan · bukan foto resep ini";
        try(var p=c.prepareStatement("SELECT image_path,caption FROM menu_presentation WHERE menu_id=?")){p.setInt(1,menu);try(var r=p.executeQuery()){if(r.next()){image=r.getString(1);caption=r.getString(2);parts.add(image);parts.add(caption);}}}
        return new Mapped(composition,review,digest(parts),Map.copyOf(ingredients),image,caption);
    }
    public static void save(Connection c,WriteTransaction tx,SessionAccess access,int menu,Composition composition)throws SQLException{
        access.requireAdmin();requireMenu(c,menu,false);
        try(var p=c.prepareStatement("SELECT state FROM content_version WHERE id=?")){p.setInt(1,menu);try(var r=p.executeQuery()){r.next();if(!r.getString(1).equals("DRAFT"))throw new IllegalArgumentException("Pemetaan hanya ditulis bersama draft baru.");}}
        try(var p=c.prepareStatement("INSERT INTO menu_composition(menu_id,completeness,recipe_servings,source,review_hash) VALUES(?,?,?,?,NULL)")){p.setInt(1,menu);p.setString(2,composition.completeness().name());p.setObject(3,composition.servings());p.setString(4,composition.source());tx.execute(p);}
        int position=0;for(var line:composition.lines()){
            if(line.ingredientId()!=null)IngredientService.get(c,line.ingredientId());
            if(line.foodId()!=null){try(var p=c.prepareStatement("SELECT version FROM food_catalog WHERE food_id=?")){p.setString(1,line.foodId());try(var r=p.executeQuery()){if(!r.next()||!Objects.equals(r.getString(1),line.foodVersion()))throw new IllegalArgumentException("Pemetaan katalog gizi berubah; pilih dan tinjau ulang.");}}}
            if(line.nutritionVerified()&&(line.foodId()==null||line.basisSource().isBlank()))throw new IllegalArgumentException("Konfirmasi basis gizi memerlukan item katalog dan sumber satuan per 100 g.");
            try(var p=c.prepareStatement("INSERT INTO menu_ingredient(menu_id,position,ingredient_id,unresolved_name,quantity,unit,grams_per_unit,conversion_source,food_id,food_version,nutrition_verified,basis_source) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")){
                p.setInt(1,menu);p.setInt(2,position++);p.setString(3,line.ingredientId());p.setString(4,line.unresolvedName());p.setObject(5,line.quantity());p.setString(6,line.unit());p.setObject(7,line.gramsPerUnit());p.setString(8,line.conversionSource());p.setString(9,line.foodId());p.setString(10,line.foodVersion());p.setBoolean(11,line.nutritionVerified());p.setString(12,line.basisSource());tx.execute(p);
            }
        }
    }
    public static void review(Connection c,WriteTransaction tx,SessionAccess access,int menu)throws SQLException{
        access.requireAdmin();var mapped=load(c,menu);
        if(mapped.composition().completeness()!=Completeness.COMPLETE)return;
        validateComplete(mapped);
        try(var p=c.prepareStatement("UPDATE menu_composition SET review_hash=? WHERE menu_id=?")){p.setString(1,mapped.currentHash());p.setInt(2,menu);if(tx.execute(p)!=1)throw new SQLException();}
    }
    public static void validateComplete(Mapped mapped){
        var c=mapped.composition();if(c.servings()==null||c.source().isBlank()||c.lines().isEmpty()||c.lines().stream().anyMatch(l->l.ingredientId()==null||!l.unresolvedName().isBlank()||!mapped.ingredients().get(l.ingredientId()).reviewed()))throw new IllegalArgumentException("Bahan lengkap memerlukan porsi hasil, sumber, seluruh identitas bahan dan peninjauan pemetaannya.");
    }
}
