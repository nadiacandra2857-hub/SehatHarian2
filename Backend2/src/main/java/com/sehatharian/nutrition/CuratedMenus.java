package com.sehatharian.nutrition;

import com.sehatharian.content.ContentService;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.AuthService;
import static com.sehatharian.nutrition.MenuModels.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicit authenticated import. Same CMS transactions/role checks; no startup hook or publication. */
public final class CuratedMenus extends MenuStore {
    public record Menu(String key,String version,String hash,String title,String body,Composition composition,String image,String caption){}
    public record Package(List<Ingredient> ingredients,List<Menu> menus,Properties input){public Package{ingredients=List.copyOf(ingredients);menus=List.copyOf(menus);}}
    public record Plan(List<String> changes,List<String> conflicts){public Plan{changes=List.copyOf(changes);conflicts=List.copyOf(conflicts);}public boolean ready(){return conflicts.isEmpty();}}
    public CuratedMenus(SessionAccess access){super(access);}
    private static String required(Properties p,String key){String v=p.getProperty(key);if(v==null||v.length()>32000)throw new IllegalArgumentException("Missing/oversized import field: "+key);return v;}
    private static int count(Properties p,String key,int max){int n=Integer.parseInt(required(p,key));if(n<1||n>max)throw new IllegalArgumentException("Invalid count "+key);return n;}
    private static Double number(Properties p,String key){String v=p.getProperty(key);return v==null?null:Double.valueOf(v);}
    public static Package read(Path path)throws Exception {
        if(Files.size(path)>2_000_000)throw new IllegalArgumentException("Import package too large");
        Properties p=new Properties();try(var in=Files.newInputStream(path)){p.loadFromXML(in);}if(!required(p,"format").equals("1"))throw new IllegalArgumentException("Unsupported import format");
        List<Ingredient> ingredients=new ArrayList<>();for(int n=0;n<count(p,"ingredients",100);n++){
            String k="i."+n+".";String aliases=required(p,k+"aliases");ingredients.add(new Ingredient(required(p,k+"id"),required(p,k+"name"),Origin.valueOf(required(p,k+"origin")),Assessment.valueOf(required(p,k+"assessment")),MenuModels.allergens(required(p,k+"allergens")),required(p,k+"source"),required(p,k+"basis"),0,aliases.isBlank()?List.of():List.of(aliases.split("\n"))));
        }
        List<Menu> menus=new ArrayList<>();for(int n=0;n<count(p,"menus",50);n++){
            String k="m."+n+".";List<Line> lines=new ArrayList<>();for(int j=0;j<count(p,k+"lines",50);j++){
                String l=k+"l."+j+".";lines.add(new Line(p.getProperty(l+"ingredient_id"),required(p,l+"unresolved_name"),number(p,l+"quantity"),required(p,l+"unit"),number(p,l+"grams_per_unit"),required(p,l+"conversion_source"),p.getProperty(l+"food_id"),p.getProperty(l+"food_version"),Boolean.parseBoolean(required(p,l+"nutrition_verified")),required(p,l+"basis_source")));
            }
            var composition=new Composition(Completeness.valueOf(required(p,k+"completeness")),number(p,k+"servings"),required(p,k+"source"),lines);
            var m=new Menu(required(p,k+"key"),required(p,k+"version"),required(p,k+"hash"),required(p,k+"title"),required(p,k+"body"),composition,required(p,k+"image"),required(p,k+"caption"));
            ContentService.validate(ContentService.Kind.MENU,m.title(),m.body(),List.of());if(!m.image().matches("/images/(menus/[a-z0-9-]+|home/nutrition)\\.jpg")||m.caption().isBlank()||m.caption().length()>255||!m.key().matches("curated:[a-z0-9._:-]{1,140}")||!m.version().matches("[a-zA-Z0-9._:-]{1,80}")||!m.hash().matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid import identity");
            // Bind the receipt to the parsed payload too, not only a caller-supplied JSON digest.
            Set<String> prefixes=new TreeSet<>();prefixes.add(k);
            Set<String> used=new HashSet<>();for(var line:lines)if(line.ingredientId()!=null)used.add(line.ingredientId());
            for(int j=0;j<ingredients.size();j++)if(used.contains(ingredients.get(j).id()))prefixes.add("i."+j+".");
            List<String> payload=new ArrayList<>();p.stringPropertyNames().stream().filter(v->prefixes.stream().anyMatch(v::startsWith)).sorted().forEach(v->{payload.add(v);payload.add(p.getProperty(v));});
            menus.add(new Menu(m.key(),m.version(),MenuModels.digest(payload),m.title(),m.body(),m.composition(),m.image(),m.caption()));
        }
        if(ingredients.stream().map(Ingredient::id).distinct().count()!=ingredients.size()||menus.stream().map(Menu::key).distinct().count()!=menus.size())throw new IllegalArgumentException("Duplicate package identities");
        return new Package(ingredients,menus,p);
    }
    /** Read-only DB transaction: no INSERT, migration, preference or review side effects. */
    public Plan plan(Package pack)throws SQLException {
        access.requireAdmin();return transaction(false,(c,tx)->{
            access.requireAdmin();List<String> changes=new ArrayList<>(),conflicts=new ArrayList<>();
            Map<String,String> aliases=new HashMap<>();Set<String> ids=new HashSet<>();
            for(var i:pack.ingredients()){
                ids.add(i.id());if(!i.id().matches("[a-z0-9][a-z0-9._:-]{0,79}")||!i.reviewed())conflicts.add("Invalid/unreviewed definition "+i.id());
                try(var p=c.prepareStatement("SELECT ingredient_id FROM ingredient_definition WHERE ingredient_id=?")){p.setString(1,i.id());try(var r=p.executeQuery()){if(r.next()){if(!IngredientService.sameDefinition(IngredientService.get(c,i.id()),i))conflicts.add("Admin definition differs: "+i.id());else changes.add("KEEP ingredient "+i.id());}else changes.add("ADD ingredient "+i.id());}}
                Set<String> keys=new HashSet<>(i.aliases());keys.add(i.id());keys.add(i.name());for(String value:keys){String key=normalize(value);String previous=aliases.putIfAbsent(key,i.id());String existing=IngredientService.resolve(c,key);if(previous!=null&&!previous.equals(i.id())||existing!=null&&!existing.equals(i.id()))conflicts.add("Alias conflict: "+key);}
            }
            int index=0;for(var m:pack.menus()){
                boolean exists=false;try(var p=c.prepareStatement("SELECT menu_id,payload_hash FROM curated_menu_import WHERE item_key=? AND source_version=?")){p.setString(1,m.key());p.setString(2,m.version());try(var r=p.executeQuery()){if(r.next()){exists=true;if(!r.getString(2).equals(m.hash()))conflicts.add("Same version has changed payload: "+m.key());else changes.add("KEEP menu "+m.key()+" id="+r.getInt(1));}}}
                if(!exists){
                    String key=UUID.nameUUIDFromBytes(("SehatHarian:"+m.key()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                    Integer head=null,imported=null;
                    try(var p=c.prepareStatement("SELECT id FROM content_version WHERE content_key=? ORDER BY revision DESC LIMIT 1")){p.setString(1,key);try(var r=p.executeQuery()){if(r.next())head=r.getInt(1);}}
                    try(var p=c.prepareStatement("SELECT menu_id FROM curated_menu_import WHERE item_key=? ORDER BY import_id DESC LIMIT 1")){p.setString(1,m.key());try(var r=p.executeQuery()){if(r.next())imported=r.getInt(1);}}
                    if(head!=null&&!head.equals(imported))conflicts.add("Admin revision exists: "+m.key());else changes.add((head==null?"ADD DRAFT ":"ADD DRAFT revision ")+m.key());
                }
                int position=0;for(var l:m.composition().lines()){
                    if(l.ingredientId()!=null&&!ids.contains(l.ingredientId()))conflicts.add("Unknown package ingredient: "+l.ingredientId());
                    if(l.foodId()!=null)try(var p=c.prepareStatement("SELECT name,version,nutrients,source FROM food_catalog WHERE food_id=?")){p.setString(1,l.foodId());try(var r=p.executeQuery()){
                        String prefix="m."+index+".l."+position+".";
                        if(!r.next()||!Objects.equals(r.getString(1),pack.input().getProperty(prefix+"food_name"))||!Objects.equals(r.getString(2),l.foodVersion())||!Objects.equals(r.getString(3),pack.input().getProperty(prefix+"food_nutrients"))||!r.getString(4).contains("upstream: "+pack.input().getProperty(prefix+"food_source")))conflicts.add("Catalogue missing/changed; import/review exact food basis: "+l.foodId());
                    }}position++;
                }index++;
            }
            return new Plan(changes,conflicts);
        });
    }
    public List<Integer> apply(Package pack)throws SQLException {
        access.requireAdmin();var plan=plan(pack);if(!plan.ready())throw new IllegalArgumentException(String.join("\n",plan.conflicts()));
        var ingredients=new IngredientService(access);for(var i:pack.ingredients())ingredients.importDefinition(i);
        var cms=new ContentService(access);List<Integer> ids=new ArrayList<>();for(var m:pack.menus())ids.add(cms.importMenu(m.key(),m.version(),m.hash(),m.title(),m.body(),m.composition(),m.image(),m.caption()));return List.copyOf(ids);
    }
    public static void main(String[] args)throws Exception {
        List<String> options=List.of(args);boolean apply=options.contains("--apply");if(options.stream().anyMatch(v->!Set.of("--apply","--dry-run").contains(v))||options.contains("--apply")&&options.contains("--dry-run"))throw new IllegalArgumentException("Use --dry-run (default) OR --apply");
        String email=System.getenv("MENU_ADMIN_EMAIL"),password=System.getenv("MENU_ADMIN_PASSWORD");
        var console=System.console();if(email==null&&console!=null)email=console.readLine("Email admin: ");if(password==null&&console!=null)password=new String(console.readPassword("Password admin: "));
        if(email==null||password==null)throw new IllegalArgumentException("Set MENU_ADMIN_EMAIL and MENU_ADMIN_PASSWORD for this explicit command only.");
        var session=new UserSession();new AuthService(new PenggunaRepository(),session).login(email,password);var service=new CuratedMenus(new SessionAccess(session));
        try{var pack=read(Path.of(".runtime/menus.xml"));var plan=service.plan(pack);String database;try(var c=service.open()){database=c.getCatalog();}System.out.println("DRY-RUN database="+database);plan.changes().forEach(System.out::println);plan.conflicts().forEach(v->System.out.println("BLOCK "+v));if(!plan.ready())throw new IllegalArgumentException("Resolve conflicts before apply; no import writes performed.");if(apply)System.out.println("IMPORTED DRAFT ids="+service.apply(pack));else System.out.println("No database writes. Use --apply explicitly after review.");}finally{session.logout();}
    }
}
