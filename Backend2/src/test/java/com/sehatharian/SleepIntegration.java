package com.sehatharian;
import com.sehatharian.config.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
/** Opt-in real database checks; synthetic accounts only in guarded schema. */
public final class SleepIntegration {
 static int checks;static void ok(boolean b,String name){if(!b)throw new AssertionError(name);System.out.println("PASS "+(++checks)+" "+name);}
 static UserSession login(String email)throws Exception{var s=new UserSession();new AuthService(new PenggunaRepository(),s).login(email,"fixture123");return s;}
 static void account(String email)throws Exception{new AuthService(new PenggunaRepository(),new UserSession()).register("UJI tidur sintetis",email,"fixture123","fixture123");}
 static long count(String query)throws Exception{try(var c=DatabaseConfig.getConnection();var p=c.createStatement();var r=p.executeQuery(query)){r.next();return r.getLong(1);}}
 static Connection faulty(boolean afterCommit)throws SQLException{var c=DatabaseConfig.getConnection();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(o,m,args)->{try{if(m.getName().equals("commit")){if(afterCommit)c.commit();throw new SQLException("UJI commit response");}return m.invoke(c,args);}catch(InvocationTargetException e){throw e.getCause();}});}
 public static void main(String[] args)throws Exception{
  if(!Objects.toString(System.getenv("DB_NAME"),"").matches("sehatharian_test_sleep_[a-z0-9_]+"))throw new SecurityException("Isolated sleep schema required");
  DatabaseBootstrap.initialize();var day=LocalDate.now(PengingatTidurService.ZONE).minusDays(1);
  if(args.length>0&&args[0].equals("reopen")){var a=new SessionAccess(login("sleep@example.test"));ok(!new ActualSleepRepository(a).episodes().isEmpty()&&!new SleepEvaluationService(a).history().isEmpty(),"fresh JVM retains actual records/evaluations");return;}
  account("sleep@example.test");account("other@example.test");var session=login("sleep@example.test");var a=new SessionAccess(session);var b=new SessionAccess(login("other@example.test"));var repo=new ActualSleepRepository(a);var eval=new SleepEvaluationService(a);
  if(args.length>0&&args[0].equals("browser-fixture")){try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("UPDATE pengguna SET onboarding_selesai=TRUE,golongan_usia='dewasa',golongan_kesibukan='dalam_rumah',waktu_luang_menit=60 WHERE id=?")){p.setInt(1,a.owner());p.executeUpdate();}repo.record(day.minusDays(1).atTime(0,0),day.minusDays(1).atTime(4,0));repo.record(day.minusDays(2).atTime(0,0),day.minusDays(2).atTime(4,0));System.out.println("BROWSER FIXTURE ready, no plan or sound pre-enabled");return;}
  var plan=new TidurService(a);ok(plan.getJadwalTerbaru(a.owner())==null,"schedule empty distinct from active");var first=plan.catatJamTidur(a.owner(),day,LocalTime.of(23,0),LocalTime.of(6,0),true);ok(first.getDurasiJam()==7,"cross midnight planned duration");var newer=plan.catatJamTidur(a.owner(),day.minusDays(1),LocalTime.of(22,0),LocalTime.of(6,0),true);ok(plan.getJadwalTerbaru(a.owner()).getId()==newer.getId(),"last saved active plan instead of latest date");plan.updatePengingat(newer.getId(),false);ok(!plan.getJadwalTerbaru(a.owner()).isPengingatAktif(),"disabled persisted");plan.updatePengingat(newer.getId(),true);
  var provider=new SqlSleepScheduleProvider(session);var schedule=provider.latest(a.owner());ok(provider.claim(a.owner(),schedule,day.atTime(22,0))&&!provider.claim(a.owner(),schedule,day.atTime(22,0)),"one automatic audio claim across tabs");ok(new SqlSleepScheduleProvider(login("other@example.test")).latest(b.owner())==null,"schedule owner isolation");plan.hapusCatatan(newer.getId());ok(plan.getJadwalTerbaru(a.owner())==null,"deleting active plan never revives older schedule");
  UUID op=UUID.randomUUID();int id=repo.record(op,day.atTime(0,0),day.atTime(4,59));ok(id==repo.record(op,day.atTime(0,0),day.atTime(4,59)),"stable operation retry reuses actual record");ok(count("SELECT COUNT(*) FROM sleep_actual")==1&&count("SELECT COUNT(*) FROM sleep_record_receipt")==1&&count("SELECT COUNT(*) FROM sleep_evaluation_request")==1,"record receipt and durable request committed together");
  var r=eval.process(day);ok(r.state().equals("WAITING"),"under5h with incomplete data holds advice");eval.process(day);ok(count("SELECT COUNT(*) FROM sleep_evaluation_result")==1,"retry no duplicate evaluation");
  repo.replace(id,day.atTime(0,0),day.atTime(5,0));ok(eval.history().getFirst().stale(),"source edit marks retained historical result stale");ok(eval.process(day).state().equals("NO_TRIGGER"),"exactly5h is not trigger");repo.replace(id,day.atTime(0,0),day.atTime(6,0));ok(eval.process(day).state().equals("NO_TRIGGER"),"above5h not trigger");
  repo.replace(id,day.atTime(0,0),day.atTime(3,0));repo.record(day.atTime(7,0),day.atTime(9,0));ok(eval.process(day).state().equals("NO_TRIGGER"),"two short episodes combined by wake date prevent false daily trigger");
  ok(count("SELECT COUNT(*) FROM sleep_actual WHERE trigger_name='tidur_kurang'")==0,"legacy trigger follows daily sum, not each short episode");
  var empty=eval.process(day.minusDays(20));ok(empty.state().equals("NO_DATA")&&empty.details().contains("bukan nol"),"empty date remains unknown instead of zero");
  long invalidBefore=count("SELECT COUNT(*) FROM sleep_actual");
  try{repo.record(day.atTime(0,30),day.atTime(1,30));throw new AssertionError();}catch(IllegalArgumentException expected){ok(true,"overlap rejected");}
  try{repo.record(day.atTime(12,0),day.atTime(12,29));throw new AssertionError();}catch(IllegalArgumentException expected){ok(true,"under30min rejected");}
  try{repo.record(day.atTime(0,0),day.atTime(21,0));throw new AssertionError();}catch(IllegalArgumentException expected){ok(true,"over20h rejected");}
  ok(count("SELECT COUNT(*) FROM sleep_actual")==invalidBefore,"invalid actual records never commit");
  ok(new ActualSleepRepository(b).episodes().isEmpty()&&new ActualSleepRepository(b).receipt(op).isEmpty()&&new SleepEvaluationService(b).history().isEmpty(),"private sleep receipt evaluation isolated by verified owner");ok(!new ActualSleepRepository(b).deleteActual(id),"other owner cannot delete actual sleep");
  long before=count("SELECT COUNT(*) FROM sleep_actual");try{new ActualSleepRepository(a,()->faulty(false)).record(UUID.randomUUID(),day.minusDays(2).atTime(0,0),day.minusDays(2).atTime(4,0));throw new AssertionError();}catch(SQLException expected){}ok(count("SELECT COUNT(*) FROM sleep_actual")==before,"failed commit before database commit leaves no partial record");
  UUID unknown=UUID.randomUUID();try{new ActualSleepRepository(a,()->faulty(true)).record(unknown,day.minusDays(3).atTime(0,0),day.minusDays(3).atTime(4,0));throw new AssertionError();}catch(SQLException expected){}ok(repo.receipt(unknown).isPresent(),"lost commit response resolved by owner receipt");repo.record(unknown,day.minusDays(3).atTime(0,0),day.minusDays(3).atTime(4,0));ok(count("SELECT COUNT(*) FROM sleep_actual")==before+1,"unknown commit retry does not duplicate");
  var pool=Executors.newFixedThreadPool(2);UUID concurrent=UUID.randomUUID();try{Callable<Integer> command=()->new ActualSleepRepository(a).record(concurrent,day.minusDays(4).atTime(0,0),day.minusDays(4).atTime(4,0));var one=pool.submit(command);var two=pool.submit(command);ok(one.get().equals(two.get()),"two concurrent same operation one record");}finally{pool.shutdownNow();}
  long resultsBefore=count("SELECT COUNT(*) FROM sleep_evaluation_result");try{new SleepEvaluationService(a,()->faulty(false)).process(day.minusDays(4));throw new AssertionError();}catch(SQLException expected){}ok(count("SELECT COUNT(*) FROM sleep_evaluation_result")==resultsBefore&&repo.receipt(concurrent).isPresent(),"evaluation failure retains actual record and durable pending request");
  ok(eval.requests().stream().anyMatch(q->q.day().equals(day.minusDays(4))&&q.state().equals("PENDING")),"durable PENDING visible after evaluation failure");
  eval.pending();ok(eval.requests().stream().noneMatch(q->q.state().equals("PENDING")),"retry processes committed pending requests");
  try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("UPDATE pengguna SET onboarding_selesai=TRUE,golongan_usia='dewasa',golongan_kesibukan='dalam_rumah',waktu_luang_menit=60 WHERE id=?")){p.setInt(1,a.owner());p.executeUpdate();}
  ok(eval.process(day).state().equals("NO_TRIGGER"),"complete profile does not change5h factual rule");
  repo.replace(id,day.atTime(0,0),day.atTime(2,0));ok(eval.process(day).state().equals("READY"),"three recorded days and saved complete profile permit contextual advice");
  repo.deleteActual(id);ok(repo.receipt(op).orElseThrow().id()==null,"deletion preserves operation tombstone");try{repo.record(op,day.atTime(0,0),day.atTime(4,59));throw new AssertionError();}catch(IllegalStateException expected){ok(true,"late retry cannot recreate deleted record");}
  ok(eval.history().stream().anyMatch(SleepEvaluationService.Result::stale),"delete retains stale historical evaluation");
  var evalPool=Executors.newFixedThreadPool(2);try{eval.process(day.minusDays(4));long n=count("SELECT COUNT(*) FROM sleep_evaluation_result");Callable<SleepEvaluationService.Result> work=()->new SleepEvaluationService(a).process(day.minusDays(4));var one=evalPool.submit(work);var two=evalPool.submit(work);ok(one.get().revision()==two.get().revision(),"concurrent evaluation retry shares revision");ok(count("SELECT COUNT(*) FROM sleep_evaluation_result")==n,"concurrent evaluation creates no duplicate result");}finally{evalPool.shutdownNow();}
  session.logout();try{repo.episodes();throw new AssertionError();}catch(SecurityException expected){ok(true,"old identity rejects callback reads and writes");}
  System.out.println("ALL "+checks+" SLEEP JDBC CHECKS PASSED");
 }
}
