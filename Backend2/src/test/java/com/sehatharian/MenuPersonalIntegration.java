package com.sehatharian;

import com.sehatharian.config.*;
import com.sehatharian.content.ContentService;
import com.sehatharian.content.ContentService.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.nutrition.*;
import com.sehatharian.storage.*;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;

/** Explicitly synthetic acceptance data, guarded disposable schema only. No production content. */
public final class MenuPersonalIntegration {
    static int checks;static final LocalDate TODAY=LocalDate.now();
    static final String SOURCE="SINTETIS UJI — bukan resep/nilai gizi nyata; fixture menu-personal v1";
    static void ok(boolean value,String description){if(!value)throw new AssertionError(description);System.out.println("PASS "+(++checks)+" "+description);}
    static void sql(String query)throws Exception{try(var c=DatabaseConfig.getConnection();var p=c.createStatement()){p.executeUpdate(query);}}
    static long scalar(String query)throws Exception{try(var c=DatabaseConfig.getConnection();var p=c.createStatement();var r=p.executeQuery(query)){r.next();return r.getLong(1);}}
    static UserSession login(String email)throws Exception{var s=new UserSession();new AuthService(new PenggunaRepository(),s).login(email,"fixture123");return s;}
    static SessionAccess access(String email)throws Exception{return new SessionAccess(login(email));}
    static void account(String email,String role)throws Exception{
        try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("INSERT INTO pengguna(nama,email,password_hash,role,onboarding_selesai) VALUES(?,?,?,?,TRUE)")){p.setString(1,"Menu "+role);p.setString(2,email);p.setString(3,com.sehatharian.util.PasswordUtil.hashPassword("fixture123"));p.setString(4,role);p.executeUpdate();}
    }
    static void records(SessionAccess a)throws Exception{for(int i=1;i<=3;i++)new KebiasaanService(a).tambahMakan(a.owner(),TODAY.minusDays(i),"sarapan","Catatan lama sintetis","Tanpa rincian bahan");}
    static void ingredient(IngredientService s,String id,String name,Origin origin,Set<Allergen> categories,List<String> aliases)throws Exception{s.save(id,name,origin,Assessment.REVIEWED,categories,aliases,SOURCE+" | "+ALLERGEN_SOURCE,"Asal dan kategori ditentukan eksplisit oleh fixture. Sembilan kategori rujukan FDA, bukan daftar menyeluruh.");}
    static void food(String id,String name,double calories)throws Exception{
        Map<String,Double> values=new HashMap<>();for(String key:Nutrients.KEYS)values.put(key,key.equals("calories")?calories:1.0);
        try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("INSERT INTO food_catalog(food_id,name,category,nutrients,source,license,version) VALUES(?,?,'SINTETIS',?,?,'synthetic-test-only', 'fixture-v1')")){p.setString(1,id);p.setString(2,name);p.setString(3,new Nutrients(values).encode());p.setString(4,SOURCE+" per 100 g");p.executeUpdate();}
    }
    static Line line(String id,String food,Double qty,String unit){return new Line(id,"",qty,unit,null,"",food,food==null?null:"fixture-v1",food!=null,food==null?"":SOURCE+" identitas dan basis per 100 g diverifikasi dalam fixture");}
    static Composition recipe(Line... lines){return new Composition(Completeness.COMPLETE,1.0,SOURCE,List.of(lines));}
    static int publish(ContentService cms,String title,Composition comp)throws Exception{int id=cms.saveMenuDraft(null,title,SOURCE,comp);cms.transition(id,State.REVIEWED);cms.transition(id,State.PUBLISHED);return id;}
    static MenuPreferences.Profile prefs(MenuPreferences p,Diet diet,Set<Allergen> categories)throws Exception{return p.save(diet,Declaration.NONE,categories.isEmpty()?Declaration.NONE:Declaration.SELECTED,Set.of(),Set.of(),categories,"","");}
    static boolean includes(PersonalMenus p,int id)throws Exception{return p.recommend(TODAY).items().stream().anyMatch(v->v.id()==id);}
    @FunctionalInterface interface Task {void run()throws Exception;}
    static void rejects(Class<? extends Exception> kind,Task task,String label)throws Exception{try{task.run();throw new AssertionError(label);}catch(Exception e){if(!kind.isInstance(e))throw e;ok(true,label);}}
    static void browserFixture()throws Exception{
        
        sql("UPDATE food_catalog SET version='fixture-v1' WHERE food_id='uji-food-rice'");
        Map<String,Double> reset=new HashMap<>();for(String key:Nutrients.KEYS)reset.put(key,key.equals("calories")?100.0:1.0);
        try(var c=DatabaseConfig.getConnection();var ps=c.prepareStatement("UPDATE food_catalog SET nutrients=? WHERE food_id='uji-food-rice'")){ps.setString(1,new Nutrients(reset).encode());ps.executeUpdate();}
        account("ui@example.test","user");var a=access("ui@example.test");records(a);
        var cms=new ContentService(access("admin@example.test"));
        for(var item:cms.published(Kind.MENU))cms.transition(item.id(),State.ARCHIVED);
        publish(cms,"[UJI] Beras dasar",recipe(line("uji-rice","uji-food-rice",100.0,"g")));
        publish(cms,"[UJI] Susu beras",recipe(line("uji-rice","uji-food-rice",100.0,"g"),line("uji-milk","uji-food-milk",100.0,"g")));
        int draft=cms.saveMenuDraft(null,"[UJI] Menu JPro",SOURCE+" — lengkapi melalui editor JPro",Composition.unknown());
        System.out.println("BROWSER_FIXTURE owner="+a.owner()+" draft="+draft+" date="+TODAY+" accounts ui@example.test/admin@example.test password fixture123");
    }
    public static void main(String[] args)throws Exception{
        String db=System.getenv("DB_NAME");if(db==null||!db.matches("sehatharian_test_menu_[a-z0-9_]+"))throw new SecurityException("New isolated menu schema required");
        if(args.length>0&&args[0].equals("browser-fixture")){browserFixture();return;}
        if(args.length>0&&args[0].equals("reopen")){var a=access("alpha@example.test");var rows=new NutritionService(a).history(TODAY);ok(rows.stream().anyMatch(v->v.portions()!=null&&v.details().contains("Beras")),"fresh JVM reads durable immutable recipe snapshot");return;}
        DatabaseBootstrap.initialize();DatabaseBootstrap.initialize();
        ok(scalar("SELECT COUNT(*) FROM ingredient_definition")==0&&scalar("SELECT COUNT(*) FROM menu_composition")==0,"repeat additive migration seeds no unverified recipes or ingredient mapping");
        account("admin@example.test","admin");account("alpha@example.test","user");account("beta@example.test","user");
        var a=access("alpha@example.test");var b=access("beta@example.test");var admin=access("admin@example.test");
        var cms=new ContentService(admin);var ingredients=new IngredientService(admin);var p=new MenuPreferences(a);var menus=new PersonalMenus(a);
        ingredient(ingredients,"uji-rice","Beras",Origin.PLANT,Set.of(),List.of("rice","beras putih"));
        ingredient(ingredients,"uji-milk","Susu sapi",Origin.DAIRY,Set.of(Allergen.MILK),List.of("milk"));
        ingredient(ingredients,"uji-egg","Telur ayam",Origin.EGG,Set.of(Allergen.EGG),List.of("egg"));
        ingredient(ingredients,"uji-peanut","Kacang tanah",Origin.PLANT,Set.of(Allergen.PEANUT),List.of("groundnut"));
        ingredient(ingredients,"uji-meat","Daging sapi",Origin.MEAT,Set.of(),List.of());
        ingredients.save("uji-unknown","Bahan belum ditinjau",Origin.UNKNOWN,Assessment.UNREVIEWED,Set.of(),List.of(),"","");
        food("uji-food-rice","Beras sintetis",100);food("uji-food-milk","Susu sintetis",50);food("uji-food-egg","Telur sintetis",200);
        try(var c=DatabaseConfig.getConnection()){ok("uji-peanut".equals(IngredientService.resolve(c,"  GROUNDNUT  "))&&IngredientService.resolve(c,"groundnut sauce")==null,"curated exact normalized alias resolves without name guessing");}
        rejects(IllegalArgumentException.class,()->ingredient(ingredients,"uji-other","Lain",Origin.PLANT,Set.of(),List.of("rice")),"ambiguous alias rejected atomically");
        rejects(SecurityException.class,()->new IngredientService(a).save("bad","Bad",Origin.PLANT,Assessment.REVIEWED,Set.of(),List.of(),SOURCE,SOURCE),"regular user cannot manage ingredient dictionary");
        rejects(SecurityException.class,()->new ContentService(a).saveMenuDraft(null,"Bad",SOURCE,recipe(line("uji-rice",null,10.0,"g"))),"regular user cannot manage menu mapping");
        rejects(SecurityException.class,()->new MenuComposition(a).admin(1),"regular user cannot inspect admin mapping");
        int rice=publish(cms,"Beras uji",recipe(line("uji-rice","uji-food-rice",100.0,"g")));
        int milk=publish(cms,"Susu beras uji",recipe(line("uji-rice","uji-food-rice",100.0,"g"),line("uji-milk","uji-food-milk",100.0,"g")));
        int egg=publish(cms,"Telur beras uji",recipe(line("uji-egg","uji-food-egg",50.0,"g")));
        int peanut=publish(cms,"Kacang uji",recipe(line("uji-peanut",null,10.0,"g")));
        int meat=publish(cms,"Daging uji",recipe(line("uji-meat",null,10.0,"g")));
        int legacy=publish(cms,"Menu lama tanpa bahan",Composition.unknown());
        int incomplete=publish(cms,"Menu belum lengkap",new Composition(Completeness.INCOMPLETE,1.0,SOURCE,List.of(line("uji-rice",null,10.0,"g"))));
        int bad=cms.saveMenuDraft(null,"Bahan misterius",SOURCE,recipe(new Line(null,"saus rahasia",null,"unknown",null,"",null,null,false,"")));
        rejects(IllegalArgumentException.class,()->cms.transition(bad,State.REVIEWED),"complete declaration cannot bypass unresolved ingredient");
        int unreviewed=cms.saveMenuDraft(null,"Bahan belum tinjau",SOURCE,recipe(line("uji-unknown",null,10.0,"g")));
        rejects(IllegalArgumentException.class,()->cms.transition(unreviewed,State.REVIEWED),"unreviewed ingredient cannot be personalized");
        ok(p.get().diet()==Diet.UNKNOWN&&p.get().restrictionStatus()==Declaration.UNKNOWN,"missing preferences stay unknown, not explicit no restrictions");
        prefs(p,Diet.OMNIVORE,Set.of());ok(menus.recommend(TODAY).items().isEmpty(),"saved preferences alone do not bypass recorded-history gate");records(a);
        sql("UPDATE pengguna SET onboarding_selesai=FALSE WHERE id="+a.owner());ok(menus.recommend(TODAY).state().equals("INSUFFICIENT"),"incomplete saved profile holds personal results");sql("UPDATE pengguna SET onboarding_selesai=TRUE WHERE id="+a.owner());
        ok(includes(menus,rice)&&includes(menus,milk)&&includes(menus,egg)&&includes(menus,meat),"profile plus three recorded days permits known published ingredients");
        ok(!includes(menus,legacy)&&!includes(menus,incomplete)&&menus.catalog().stream().anyMatch(v->v.id()==legacy),"legacy and incomplete remain general catalog, never matched");
        prefs(p,Diet.VEGETARIAN,Set.of(Allergen.MILK,Allergen.PEANUT));
        ok(includes(menus,rice)&&includes(menus,egg)&&!includes(menus,milk)&&!includes(menus,peanut)&&!includes(menus,meat),"hard categories filtered before vegetarian ranking");
        prefs(p,Diet.VEGAN,Set.of());ok(!includes(menus,egg)&&!includes(menus,milk)&&!includes(menus,meat)&&includes(menus,rice),"vegan excludes all known animal origins");
        p.save(Diet.OMNIVORE,Declaration.SELECTED,Declaration.NONE,Set.of("uji-rice"),Set.of(),Set.of(),"","");ok(!includes(menus,rice)&&!includes(menus,milk),"preference ingredient avoidance is applied to every recipe line");
        p.save(Diet.OMNIVORE,Declaration.NONE,Declaration.SELECTED,Set.of(),Set.of(),Set.of(),"","groundnut");ok(p.get().restrictions().contains("uji-peanut")&&!includes(menus,peanut),"curated alias input persists mapped personal restriction");
        p.save(Diet.VEGETARIAN,Declaration.NONE,Declaration.SELECTED,Set.of(),Set.of(),Set.of(),"","saus rahasia");ok(!p.get().unresolved().isEmpty()&&menus.recommend(TODAY).state().equals("UNASSESSABLE"),"unmapped raw text preserved and blocks personal matching");
        prefs(p,Diet.VEGETARIAN,Set.of());var before=menus.review(rice,TODAY,"makan_siang",1);
        prefs(p,Diet.VEGETARIAN,Set.of(Allergen.MILK));rejects(PersonalMenus.Changed.class,()->menus.log(before),"preference revision invalidates old portion review");
        var valid=menus.review(rice,TODAY,"makan_siang",1);ok(valid.nutrients().values().get("calories")==100.0,"verified per100g quantity computes recipe portion estimate");
        long revision=p.get().revision();new HabitPreferences(a).save(1800,null,null);ok(p.get().revision()==revision,"hydration-only first legacy preference save does not invalidate menu choices");
        int unknownQty=publish(cms,"Jumlah belum diketahui",recipe(line("uji-rice","uji-food-rice",null,"g")));
        int unknownUnit=publish(cms,"Takaran belum diketahui",recipe(line("uji-rice","uji-food-rice",1.0,"sdm")));
        ok(menus.review(unknownQty,TODAY,"sarapan",1).nutrients().values().isEmpty()&&menus.review(unknownUnit,TODAY,"sarapan",1).nutrients().values().isEmpty(),"unknown quantity or household conversion never becomes zero or complete total");
        var conversion=new Line("uji-rice","",2.0,"sdm",10.0,SOURCE+" konversi sintetis", "uji-food-rice","fixture-v1",true,SOURCE+" per100g");
        int converted=publish(cms,"Takaran diketahui",recipe(conversion));ok(menus.review(converted,TODAY,"sarapan",0.5).nutrients().values().get("calories")==10.0,"explicit documented conversion and portions applied exactly");
        sql("UPDATE food_catalog SET nutrients='calories=200.0' WHERE food_id='uji-food-rice'");
        rejects(PersonalMenus.Changed.class,()->menus.log(valid),"changed catalog values invalidate reviewed estimate even same catalog version");
        var partialPreview=menus.review(rice,TODAY,"makan_siang",1);ok(partialPreview.nutrients().values().size()==1&&partialPreview.nutrients().display().contains("Protein: belum diketahui"),"missing nutrient keys remain unknown");
        prefs(p,Diet.VEGETARIAN,Set.of());var mixed=menus.review(milk,TODAY,"sarapan",1);ok(mixed.nutrients().values().size()==1&&mixed.nutrients().values().get("calories")==250.0,"mixed recipe omits parameters missing from even one ingredient");prefs(p,Diet.VEGETARIAN,Set.of(Allergen.MILK));var partial=menus.review(rice,TODAY,"makan_siang",1);
        long meals=scalar("SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id="+a.owner());
        PenggunaRepository.ConnectionFactory fail=()->{Connection real=DatabaseConfig.getConnection();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,values)->{
            try{Object result=method.invoke(real,values);if(method.getName().equals("prepareStatement")&&values[0].toString().startsWith("INSERT INTO menu_meal_snapshot")){PreparedStatement ps=(PreparedStatement)result;return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(x,m,v)->{if(m.getName().equals("executeUpdate"))throw new SQLException("fixture snapshot failure");try{return m.invoke(ps,v);}catch(InvocationTargetException e){throw e.getCause();}});}return result;}catch(InvocationTargetException e){throw e.getCause();}
        });};
        ok(WriteResult.attempt(()->new PersonalMenus(a,fail).log(partial)).status()==WriteResult.Status.GAGAL&&scalar("SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id="+a.owner())==meals,"meal and recipe snapshot roll back together on snapshot failure");
        int saved=menus.log(partial);String details=new NutritionService(a).history(TODAY).stream().filter(v->v.id()==saved).findFirst().orElseThrow().details();
        ok(scalar("SELECT COUNT(*) FROM menu_meal_snapshot WHERE meal_id="+saved)==1,"one atomic reviewed meal plus version/portion/nutrient snapshot");
        rejects(PersonalMenus.Changed.class,()->menus.log(partial),"same successful review becomes stale instead of repeating meal insert");
        rejects(SecurityException.class,()->new PersonalMenus(b).log(partial),"other owner cannot submit another owner's review");
        ok(new MenuPreferences(b).get().revision()==0&&new NutritionService(b).history(TODAY).isEmpty(),"other owner sees only own preferences and meal history");
        int draft=cms.saveMenuDraft(rice,"Revisi beras",SOURCE,recipe(line("uji-milk","uji-food-milk",10.0,"g")));
        ok(includes(menus,rice),"draft revision retains old published ingredient version");
        var pending=menus.review(rice,TODAY,"sarapan",1);cms.transition(draft,State.REVIEWED);cms.transition(draft,State.PUBLISHED);
        rejects(PersonalMenus.Changed.class,()->menus.log(pending),"new published recipe version invalidates old result review");
        ok(!includes(menus,rice)&&!includes(menus,draft),"revised recipe hard filters new ingredients; superseded version excluded");
        var beforeDefinition=menus.recommend(TODAY).fingerprint();ingredient(ingredients,"uji-egg","Telur ayam baru",Origin.EGG,Set.of(Allergen.EGG),List.of("egg"));
        ok(!includes(menus,egg)&&!menus.recommend(TODAY).fingerprint().equals(beforeDefinition),"ingredient review revision invalidates linked menu eligibility and result fingerprint");
        sql("UPDATE food_catalog SET version='fixture-v2' WHERE food_id='uji-food-rice'");
        ok(menus.review(converted,TODAY,"sarapan",1).nutrients().values().isEmpty(),"obsolete catalog unit version is unknown until admin remaps");
        var old=new NutritionService(a).history(TODAY).stream().filter(v->v.id()==saved).findFirst().orElseThrow();
        ok(details.equals(old.details())&&old.nutrients().values().get("calories")==200.0&&old.portions()==1&&old.grams()==null,"ingredient, recipe and catalog changes preserve old snapshot and unknown cooked weight");
        ok(new NutritionService(a).history(TODAY.minusDays(1)).getFirst().portions()==null,"legacy manual meal still readable with no fabricated composition");
        new NutritionService(a).save(TODAY,"camilan","uji-food-rice",50,"fixture-v2");ok(new NutritionService(a).history(TODAY).stream().anyMatch(v->v.grams()!=null&&v.grams()==50),"legacy food catalog gram snapshot remains readable alongside recipe portions");
        p.save(Diet.VEGAN,Declaration.SELECTED,Declaration.SELECTED,Set.of("uji-rice","uji-peanut"),Set.of("uji-milk","uji-egg","uji-meat"),Set.of(),"","");
        ok(menus.recommend(TODAY).items().isEmpty()&&menus.recommend(TODAY).state().equals("EMPTY"),"empty personal results never loosen hard restrictions");
        var retiring=login("alpha@example.test");var retired=new MenuPreferences(new SessionAccess(retiring));retiring.logout();rejects(SecurityException.class,retired::get,"old session cannot read after logout");
        rejects(SecurityException.class,()->retired.save(Diet.VEGAN,Declaration.NONE,Declaration.NONE,Set.of(),Set.of(),Set.of(),"",""),"old session cannot write after logout");
        DatabaseBootstrap.initialize();ok(new NutritionService(a).history(TODAY).stream().anyMatch(v->v.id()==saved&&v.details().equals(details)),"repeat migration preserves old recipe history snapshot");
        System.out.println("ALL "+checks+" JDBC MENU CHECKS PASSED on "+db);
    }
}
