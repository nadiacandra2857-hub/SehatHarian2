package com.sehatharian;
import com.sehatharian.config.*;
import com.sehatharian.content.ContentService;
import com.sehatharian.content.ContentService.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.nutrition.*;
import com.sehatharian.training.*;
import com.sehatharian.catalog.*;
import static com.sehatharian.nutrition.MenuModels.*;
import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Real curated content, explicitly synthetic accounts/history; isolated schema guard. */
public final class CuratedMenuIntegration {
 static int checks;
 static void ok(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+(++checks)+" "+label);}
 static long scalar(String q)throws Exception{try(var c=DatabaseConfig.getConnection();var p=c.createStatement();var r=p.executeQuery(q)){r.next();return r.getLong(1);}}
 static UserSession login(String email)throws Exception{var s=new UserSession();new AuthService(new PenggunaRepository(),s).login(email,"fixture123");return s;}
 static SessionAccess access(String email)throws Exception{return new SessionAccess(login(email));}
 static void account(String email,boolean admin)throws Exception{
  var auth=new AuthService(new PenggunaRepository(),new UserSession());auth.register("Kurasi — akun sintetis",email,"fixture123","fixture123");
  try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("UPDATE pengguna SET onboarding_selesai=TRUE,role=? WHERE email=?")){p.setString(1,admin?"admin":"user");p.setString(2,email);p.executeUpdate();}
 }
 static void records(SessionAccess a)throws Exception{for(int i=1;i<=3;i++)new KebiasaanService(a).tambahMakan(a.owner(),LocalDate.now().minusDays(i),"sarapan","Catatan sintetis untuk gerbang data","UJI profil/catatan; bukan fakta konsumsi nyata");}
 @FunctionalInterface interface Work{void run()throws Exception;}
 static void rejects(Work w,String label)throws Exception{try{w.run();throw new AssertionError(label);}catch(IllegalArgumentException|IllegalStateException|SecurityException expected){ok(true,label);}}
 static void publish(ContentService cms,int id)throws Exception{cms.transition(id,State.REVIEWED);cms.transition(id,State.PUBLISHED);}
 static void prefs(SessionAccess a,Diet d,Set<Allergen> categories)throws Exception{new MenuPreferences(a).save(d,Declaration.NONE,categories.isEmpty()?Declaration.NONE:Declaration.SELECTED,Set.of(),Set.of(),categories,"","");}
 static TrainingSnapshot snapshot(){AtomicLong time=new AtomicLong();var engine=new TrainingSession(List.of(new TrainingSession.Step(TeamExercises.ALL.getFirst().exercise(),false,60)),1,0,time::get);engine.start();time.addAndGet(1_000_000_000L);engine.pulse(engine.revision());engine.advance(engine.revision(),false);for(int i=0;i<60;i++){time.addAndGet(1_000_000_000L);engine.pulse(engine.revision());}engine.advance(engine.revision(),false);return TrainingSnapshot.capture(engine,null,null,"[UJI paket] Hasil latihan");}
 static void releaseFixture()throws Exception{
  account("release-admin@example.test",true);account("release@example.test",false);var a=access("release@example.test");records(a);prefs(a,Diet.VEGETARIAN,Set.of());new TrainingHistory(a).save(snapshot());
  var e=TrainingSession.draft(UUID.randomUUID(),List.of(new TrainingSession.Step(TeamExercises.ALL.getFirst().exercise(),false,60)),5,4,System::nanoTime);
  new TrainingCheckpoints(a).create(UUID.randomUUID(),TrainingCheckpoint.capture(e,null,null,"[UJI paket] Susunan pulih",null));System.out.println("RELEASE FIXTURE: separate synthetic account/history/checkpoint; curated import not invoked here");
 }
 public static void main(String[] args)throws Exception{
  String db=System.getenv("DB_NAME");if(db==null||!db.matches("sehatharian_test_curated_[a-z0-9_]+"))throw new SecurityException("Disposable curated schema required");
  String mode=args.length==0?"test":args[0];
  if(mode.equals("release-fixture")){releaseFixture();return;}
  if(mode.equals("release-publish")){var cms=new ContentService(access("release-admin@example.test"));for(var item:cms.adminList(Kind.MENU,""))if(item.state()==State.DRAFT)publish(cms,item.id());System.out.println("EXPLICIT FIXTURE review/publish via ContentService");return;}
  if(mode.equals("reopen")){var h=new NutritionService(access("alpha@example.test")).history(LocalDate.now());ok(h.stream().anyMatch(m->m.name().equals("Pisang beku lembut")&&m.portions()==1&&m.nutrients().values().get("calories")==89),"fresh JVM retains sourced menu snapshot after later version change");return;}
  DatabaseBootstrap.initialize();DatabaseBootstrap.initialize();ok(scalar("SELECT COUNT(*) FROM pengguna")==0&&scalar("SELECT COUNT(*) FROM aktivitas")==0&&scalar("SELECT COUNT(*) FROM kebiasaan_makan")==0,"fresh provisioning creates no demo account, personal data or curated seed");FoodImport.main(new String[]{".runtime/foods.xml"});
  account("admin@example.test",true);account("alpha@example.test",false);account("beta@example.test",false);
  var admin=access("admin@example.test");var a=access("alpha@example.test");var b=access("beta@example.test");var importer=new CuratedMenus(admin);var pack=CuratedMenus.read(Path.of(".runtime/menus.xml"));
  if(mode.equals("browser-fixture")){records(a);System.out.println("Plan="+importer.plan(pack));System.out.println("Browser drafts="+importer.apply(pack));return;}
  var cms=new ContentService(admin);int own=cms.saveMenuDraft(null,"Konten admin milik pengguna","Dipertahankan",Composition.unknown());
  long before=scalar("SELECT COUNT(*) FROM content_version");var plan=importer.plan(pack);ok(plan.ready()&&scalar("SELECT COUNT(*) FROM content_version")==before&&scalar("SELECT COUNT(*) FROM ingredient_definition")==0,"dry-run reports additions without writing any ingredient or content");
  rejects(()->new CuratedMenus(a).apply(pack),"non-admin cannot import curated content");
  var ids=importer.apply(pack);ok(ids.size()==5&&scalar("SELECT COUNT(*) FROM curated_menu_import")==5&&scalar("SELECT COUNT(*) FROM content_version WHERE state='DRAFT'")==6,"five sourced menus imported as DRAFT with durable receipts");
  ok(scalar("SELECT COUNT(*) FROM content_version WHERE id="+own+" AND title='Konten admin milik pengguna'")==1,"unrelated admin content remains unchanged");
  var again=importer.apply(pack);ok(ids.equals(again)&&scalar("SELECT COUNT(*) FROM content_version")==before+5&&scalar("SELECT MAX(revision) FROM ingredient_definition")==1,"repeat import reuses content IDs and definitions without duplicate or revision bumps");
  var tampered=(Properties)pack.input().clone();tampered.setProperty("m.0.l.0.quantity","599.0");var temp=Files.createTempFile("curated-tampered-",".xml");try{try(var output=Files.newOutputStream(temp)){tampered.storeToXML(output,"UJI normalized payload edit");}var changedInput=CuratedMenus.read(temp);ok(!importer.plan(changedInput).ready()&&scalar("SELECT COUNT(*) FROM curated_menu_import")==5,"same source version with edited normalized payload is detected independently of supplied digest");}finally{Files.deleteIfExists(temp);}
  var workers=Executors.newFixedThreadPool(2);try{var go=new CountDownLatch(1);Callable<List<Integer>> task=()->{go.await();return new CuratedMenus(admin).apply(pack);};var x=workers.submit(task);var y=workers.submit(task);go.countDown();ok(x.get().equals(y.get())&&scalar("SELECT COUNT(*) FROM curated_menu_import")==5,"two concurrent explicit imports return the same five drafts");}finally{workers.shutdownNow();}
  for(int i=0;i<ids.size();i++){var expected=pack.menus().get(i);var mapped=new MenuComposition(admin).admin(ids.get(i));ok(mapped.composition().equals(expected.composition())&&mapped.image().equals(expected.image())&&mapped.caption().equals(expected.caption()),"stored source/yield/ingredients/status/illustration match package: "+expected.title());publish(cms,ids.get(i));}
  int banana=ids.get(0),omelet=ids.get(1),salad=ids.get(2),oat=ids.get(3),rice=ids.get(4);
  ok(new MenuComposition(a).published(banana).eligible()&&new MenuComposition(a).published(omelet).eligible()&&new MenuComposition(a).published(salad).eligible(),"three reviewed complete recipes become eligible");
  ok(!new MenuComposition(a).published(oat).eligible()&&!new MenuComposition(a).published(rice).eligible(),"ambiguous dairy/fruit/soy-sauce/mixed vegetables stay INCOMPLETE general catalog");
  records(a);prefs(a,Diet.VEGETARIAN,Set.of());var menus=new PersonalMenus(a);ok(menus.recommend(LocalDate.now()).items().size()==3,"saved profile/history and vegetarian preferences yield three complete menus");
  var old=menus.review(banana,LocalDate.now(),"camilan",1);ok(old.nutrients().values().size()==8&&old.nutrients().values().get("calories")==89.0,"600 g weighed banana divided by six yields 89 kcal per selected 100 g ingredient portion");
  var two=menus.review(banana,LocalDate.now(),"camilan",2);ok(two.nutrients().values().get("calories")==178.0&&two.nutrients().values().get("carbs")==45.6,"two portions use verified per100g basis, not source generic nutrition panel");
  ok(menus.review(omelet,LocalDate.now(),"sarapan",1).nutrients().values().isEmpty()&&menus.review(salad,LocalDate.now(),"makan_siang",1).nutrients().values().isEmpty(),"complete ingredient lists can still have unknown nutrition due to unconverted measures");
  prefs(a,Diet.VEGETARIAN,Set.of(Allergen.MILK));ok(menus.recommend(LocalDate.now()).items().size()==2&&menus.recommend(LocalDate.now()).items().stream().noneMatch(m->m.id()==omelet),"milk restriction excludes sourced omelet while preserving plant recipes");
  rejects(()->menus.log(old),"changed preferences require another portion review");
  prefs(a,Diet.VEGAN,Set.of());ok(menus.recommend(LocalDate.now()).items().size()==2,"vegan selection excludes egg and dairy source ingredients");
  new MenuPreferences(a).save(Diet.VEGAN,Declaration.SELECTED,Declaration.NONE,Set.of("sh-bahan:pisang","sh-bahan:tomat"),Set.of(),Set.of(),"","");ok(menus.recommend(LocalDate.now()).items().isEmpty(),"empty sourced results preserve all restrictions");
  prefs(a,Diet.VEGETARIAN,Set.of());var reviewed=menus.review(banana,LocalDate.now(),"camilan",1);int meal=menus.log(reviewed);String historic=new NutritionService(a).history(LocalDate.now()).stream().filter(m->m.id()==meal).findFirst().orElseThrow().details();
  ok(new NutritionService(b).history(LocalDate.now()).isEmpty(),"other account cannot read sourced recipe history");
  var original=pack.menus().getFirst();var conflict=new CuratedMenus.Menu(original.key(),original.version(),"0".repeat(64),original.title(),original.body(),original.composition(),original.image(),original.caption());
  rejects(()->importer.apply(new CuratedMenus.Package(pack.ingredients(),List.of(conflict),pack.input())),"same external version with changed payload is rejected instead of silently overwritten");
  var changed=new CuratedMenus.Menu(original.key(),"test-revision-2","1".repeat(64),"[UJI] Revisi porsi",original.body(),new Composition(Completeness.COMPLETE,6.0,original.composition().source(),List.of(new Line("sh-bahan:pisang","",300.0,"g",null,"","tempolife:17366",original.composition().lines().getFirst().foodVersion(),true,original.composition().lines().getFirst().basisSource()))),original.image(),original.caption());
  var pending=menus.review(banana,LocalDate.now(),"camilan",1);int revised=importer.apply(new CuratedMenus.Package(pack.ingredients(),List.of(changed),pack.input())).getFirst();
  ok(menus.recommend(LocalDate.now()).items().stream().anyMatch(m->m.id()==banana),"new imported source version stays DRAFT and retains previous publication");publish(cms,revised);rejects(()->menus.log(pending),"published version revision invalidates old reviewed portion");
  var stored=new NutritionService(a).history(LocalDate.now()).stream().filter(m->m.id()==meal).findFirst().orElseThrow();ok(stored.details().equals(historic)&&stored.nutrients().values().get("calories")==89&&stored.portions()==1,"earlier source/version/quantity/nutrition snapshot survives revised recipe");
  cms.saveMenuDraft(revised,"Revisi admin tersendiri","UJI revisi admin",original.composition());var next=new CuratedMenus.Menu(original.key(),"test-revision-3","2".repeat(64),original.title(),original.body(),original.composition(),original.image(),original.caption());
  rejects(()->importer.apply(new CuratedMenus.Package(pack.ingredients(),List.of(next),pack.input())),"import cannot supersede an independent admin revision");
  var definition=pack.ingredients().getFirst();new IngredientService(admin).save(definition.id(),definition.name(),definition.origin(),definition.assessment(),definition.allergens(),definition.aliases(),definition.source(),definition.basis()+"\nUJI peninjauan admin baru");
  rejects(()->importer.apply(pack),"import refuses to overwrite changed ingredient definition");
  ok(scalar("SELECT COUNT(*) FROM curated_menu_import")==6,"rejections leave exactly one additional source-version receipt");
  System.out.println("ALL "+checks+" CURATED JDBC CHECKS PASSED on "+db);
 }
}
