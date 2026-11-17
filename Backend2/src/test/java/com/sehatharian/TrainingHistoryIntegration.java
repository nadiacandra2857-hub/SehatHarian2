package com.sehatharian;

import com.sehatharian.config.*;
import com.sehatharian.catalog.*;
import com.sehatharian.content.ContentService;
import com.sehatharian.content.ContentService.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.storage.*;
import com.sehatharian.training.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Opt-in integration fixture. Only a new, explicitly named disposable schema is accepted. */
public final class TrainingHistoryIntegration {
    static int checks;
    static void ok(boolean value,String description){if(!value)throw new AssertionError(description);System.out.println("PASS "+(++checks)+" "+description);}
    static long scalar(String sql)throws Exception{try(var c=DatabaseConfig.getConnection();var p=c.createStatement();var r=p.executeQuery(sql)){r.next();return r.getLong(1);}}
    static Object invoke(Object target,Method method,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}}
    static PenggunaRepository.ConnectionFactory fault(String mode){return ()->{
        Connection real=DatabaseConfig.getConnection();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            if(method.getName().equals("commit")){
                if(mode.equals("commit-before"))throw new SQLException("fixture commit not delivered");
                if(mode.equals("commit-after")){real.commit();throw new SQLException("fixture acknowledgement lost");}
            }
            Object value=invoke(real,method,args);
            if(mode.equals("step")&&method.getName().equals("prepareStatement")&&args[0].toString().startsWith("INSERT INTO training_step")){
                PreparedStatement statement=(PreparedStatement)value;
                return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{if(m.getName().equals("executeUpdate"))throw new SQLException("fixture step failure");return invoke(statement,m,a);});
            }
            return value;
        });
    };}
    static TrainingSnapshot result(Integer program,Integer version){
        AtomicLong time=new AtomicLong();var exercise=TeamExercises.ALL.get(0).exercise();
        var engine=new TrainingSession(List.of(new TrainingSession.Step(exercise,false,60),new TrainingSession.Step(exercise,true,12),new TrainingSession.Step(exercise,false,20)),1,1,time::get);
        engine.start();tick(engine,time,1);engine.advance(engine.revision(),false);tick(engine,time,60);engine.advance(engine.revision(),false);
        tick(engine,time,1);engine.advance(engine.revision(),false);time.addAndGet(750_000_000);engine.pulse(engine.revision());engine.pause();tick(engine,time,4);engine.resume();tick(engine,time,2);engine.advance(engine.revision(),false);
        tick(engine,time,1);engine.advance(engine.revision(),false);time.addAndGet(500_000_000);engine.pulse(engine.revision());engine.advance(engine.revision(),true);
        return TrainingSnapshot.capture(engine,program,version,"Program historis");
    }
    static void tick(TrainingSession e,AtomicLong t,int count){for(int i=0;i<count;i++){t.addAndGet(1_000_000_000);e.pulse(e.revision());}}
    static UserSession login(String email)throws Exception{var s=new UserSession();new AuthService(new PenggunaRepository(),s).login(email,"fixture123");return s;}
    public static void main(String[] args)throws Exception {
        String db=System.getenv("DB_NAME");if(db==null||!db.matches("sehatharian_test_history_[a-z0-9_]+"))throw new SecurityException("Disposable history schema required");
        if(args.length>0&&args[0].equals("browser-fixture")){
            var admin=login("admin@example.test");var cms=new ContentService(new SessionAccess(admin));
            int movement=cms.saveDraft(null,Kind.MOVEMENT,"Jalan tim","team-walking",List.of());cms.transition(movement,State.REVIEWED);cms.transition(movement,State.PUBLISHED);
            int program=cms.saveDraft(null,Kind.PROGRAM,"Program Riwayat","Latihan tim · 60 detik",List.of(new ContentService.Step(movement,60,false)));cms.transition(program,State.REVIEWED);cms.transition(program,State.PUBLISHED);
            System.out.println("BROWSER PROGRAM "+program);return;
        }
        if(args.length>0&&args[0].equals("reopen")){
            var session=login("alpha@example.test");var history=new TrainingHistory(new SessionAccess(session));var saved=history.list(0,0).getFirst();ok(history.detail(saved.id()).snapshot().fingerprint().equals(saved.snapshot().fingerprint()),"fresh JVM reads persisted detail after restart");return;
        }
        DatabaseBootstrap.initialize();long tables=scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()");DatabaseBootstrap.initialize();ok(tables==scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"),"additive migration is repeatable");
        ok(scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('training_session','training_step','aktivitas') AND engine='InnoDB'")==3,"actual application tables use InnoDB");
        var registrar=new AuthService(new PenggunaRepository(),new UserSession());registrar.register("History Alpha","alpha@example.test","fixture123","fixture123");registrar.register("History Beta","beta@example.test","fixture123","fixture123");
        try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("UPDATE pengguna SET onboarding_selesai=TRUE WHERE email IN ('alpha@example.test','beta@example.test')")){p.executeUpdate();}
        var a=login("alpha@example.test");var b=login("beta@example.test");var access=new SessionAccess(a);var history=new TrainingHistory(access);
        try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("INSERT INTO pengguna(nama,email,password_hash,role,onboarding_selesai) VALUES('History Admin','admin@example.test',?,'admin',TRUE)")){p.setString(1,com.sehatharian.util.PasswordUtil.hashPassword("fixture123"));p.executeUpdate();}
        var admin=login("admin@example.test");var cms=new ContentService(new SessionAccess(admin));int movement=cms.saveDraft(null,Kind.MOVEMENT,"Gerakan pertama","team-walking",List.of());cms.transition(movement,State.REVIEWED);cms.transition(movement,State.PUBLISHED);
        int program=cms.saveDraft(null,Kind.PROGRAM,"Program Uji","Sesi video 60 detik",List.of(new ContentService.Step(movement,60,false)));cms.transition(program,State.REVIEWED);cms.transition(program,State.PUBLISHED);
        var snapshot=result(program,1);ok(snapshot.activeNanos()==63_250_000_000L,"engine excludes preparation, rest and pause; retains subsecond step times");
        ok(snapshot.steps().stream().mapToLong(TrainingSnapshot.Step::activeNanos).sum()==snapshot.activeNanos(),"session nanos equal exact step sum");
        ok(snapshot.steps().get(1).repetitionConfirmed()&&snapshot.steps().get(1).confirmedRepetitions()==null&&snapshot.steps().get(1).target()==12,"manual completion is distinct from repetition target and unknown measured count");
        long activities=scalar("SELECT COUNT(*) FROM aktivitas");var saved=history.save(snapshot);
        ok(scalar("SELECT COUNT(*) FROM aktivitas")==activities+1&&scalar("SELECT COUNT(*) FROM training_session")==1&&scalar("SELECT COUNT(*) FROM training_step")==3,"one save atomically stores activity, session and all steps");
        ok(scalar("SELECT durasi_menit FROM aktivitas WHERE id="+saved.activityId())==1,"legacy minutes still floor total active seconds");
        ok(history.save(snapshot).id()==saved.id()&&scalar("SELECT COUNT(*) FROM aktivitas")==activities+1,"repeat save returns same receipt without duplicate activity");
        var modified=new TrainingSnapshot(snapshot.operationId(),snapshot.startedAt(),snapshot.endedAt(),snapshot.activeNanos(),snapshot.stopped(),program,1,"Changed payload",snapshot.steps());
        try{history.save(modified);throw new AssertionError();}catch(IllegalArgumentException expected){ok(true,"operation cannot be reused for changed snapshot");}
        var concurrent=result(null,null);var pool=Executors.newFixedThreadPool(3);try{
            var go=new CountDownLatch(1);Callable<TrainingHistory.Saved> call=()->{go.await();return history.save(concurrent);};var f=pool.submit(call);var g=pool.submit(call);go.countDown();ok(f.get().id()==g.get().id(),"two concurrent JDBC requests return one session");
        }finally{pool.shutdownNow();}
        ok(scalar("SELECT COUNT(*) FROM training_session")==2&&scalar("SELECT COUNT(*) FROM aktivitas")==activities+2,"concurrent requests insert one activity only");
        for(String field:List.of("operation_id","activity_id")){
            try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("UPDATE training_session SET "+field+"=? WHERE pengguna_id=? AND id<>?")){
                p.setObject(1,field.equals("operation_id")?snapshot.operationId().toString():saved.activityId());p.setInt(2,access.owner());p.setLong(3,saved.id());p.executeUpdate();throw new AssertionError("missing unique constraint");
            }catch(SQLException expected){ok("23000".equals(expected.getSQLState()),"database directly enforces unique "+field);}
        }
        var failed=result(null,null);var failResult=WriteResult.attempt(()->new TrainingHistory(access,fault("step")).save(failed));
        ok(failResult.status()==WriteResult.Status.GAGAL,"before-commit step failure confirms rollback");
        ok(history.status(failed.operationId()).isEmpty()&&scalar("SELECT COUNT(*) FROM training_step")==6&&scalar("SELECT COUNT(*) FROM aktivitas")==activities+2,"rollback leaves no partial activity/session/steps");
        ok(history.save(failed).snapshot().operationId().equals(failed.operationId()),"explicit retry keeps same operation id");
        var unknown=result(null,null);var uncertain=WriteResult.attempt(()->new TrainingHistory(access,fault("commit-after")).save(unknown));
        ok(uncertain.status()==WriteResult.Status.BELUM_PASTI,"lost commit acknowledgement is uncertain");
        var receipt=history.status(unknown.operationId()).orElseThrow();ok(receipt.snapshot().fingerprint().equals(unknown.fingerprint()),"owned operation status confirms exact committed snapshot");
        long after=scalar("SELECT COUNT(*) FROM aktivitas");ok(history.save(unknown).id()==receipt.id()&&scalar("SELECT COUNT(*) FROM aktivitas")==after,"retry after unknown commit cannot duplicate");
        var notCommitted=result(null,null);ok(WriteResult.attempt(()->new TrainingHistory(access,fault("commit-before")).save(notCommitted)).needsReview(),"undelivered commit also remains uncertain until checked");
        ok(history.status(notCommitted.operationId()).isEmpty(),"status confirms no committed result after rollback");history.save(notCommitted);
        var inFlight=result(null,null);var atCommit=new CountDownLatch(1);var releaseCommit=new CountDownLatch(1);
        PenggunaRepository.ConnectionFactory held=()->{
            Connection real=DatabaseConfig.getConnection();
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,values)->{
                if(method.getName().equals("commit")){atCommit.countDown();if(!releaseCommit.await(10,TimeUnit.SECONDS))throw new SQLException("fixture wait timed out");}
                return invoke(real,method,values);
            });
        };
        var statusPool=Executors.newFixedThreadPool(2);
        try{
            var writer=statusPool.submit(()->new TrainingHistory(access,held).save(inFlight));
            if(!atCommit.await(10,TimeUnit.SECONDS))throw new AssertionError("writer did not reach commit");
            var probe=statusPool.submit(()->history.status(inFlight.operationId()));Thread.sleep(200);
            ok(!probe.isDone(),"status probe waits for in-flight owned writer instead of reporting false absence");
            releaseCommit.countDown();ok(probe.get().orElseThrow().id()==writer.get().id(),"status resolves the in-flight operation after commit");
        }finally{releaseCommit.countDown();statusPool.shutdownNow();}
        var bx=new TrainingHistory(new SessionAccess(b));ok(bx.list(0,0).isEmpty()&&bx.detail(saved.id())==null&&bx.byActivity(saved.activityId())==null&&bx.status(snapshot.operationId()).isEmpty(),"other owner cannot list/read/probe private history");
        ok(!new KebiasaanService(new SessionAccess(b)).hapusAktivitas(saved.activityId())&&history.detail(saved.id()).snapshot().equals(snapshot),"other owner cannot delete linked activity or alter session history");
        int revised=cms.saveDraft(program,Kind.PROGRAM,"Program revised","Different target",List.of(new ContentService.Step(movement,300,false)));cms.transition(revised,State.REVIEWED);cms.transition(revised,State.PUBLISHED);cms.transition(revised,State.ARCHIVED);cms.transition(movement,State.ARCHIVED);
        ok(history.detail(saved.id()).snapshot().fingerprint().equals(snapshot.fingerprint()),"program revision and movement archive do not alter historic snapshot");
        new KebiasaanService(access).tambahAktivitas(access.owner(),LocalDate.now(),"Legacy training",2,"Only legacy description");var legacy=new KebiasaanService(access).getAktivitasHariIni(access.owner(),LocalDate.now()).stream().filter(x->x.getJenisAktivitas().equals("Legacy training")).findFirst().orElseThrow();
        ok(new AktivitasRepository(access).getById(legacy.getId())!=null&&history.byActivity(legacy.getId())==null,"legacy activity stays readable without reconstructing steps");
        var base=result(null,null);var missingSteps=new ArrayList<>(base.steps());var first=missingSteps.getFirst();missingSteps.set(0,new TrainingSnapshot.Step(first.movementRef(),first.name(),"/images/team/no-longer-available.jpg",first.sourceRef(),first.category(),first.repetitions(),first.target(),first.activeNanos(),first.outcome(),first.repetitionConfirmed(),first.confirmedRepetitions()));
        var missing=new TrainingSnapshot(base.operationId(),base.startedAt(),base.endedAt(),base.activeNanos(),base.stopped(),null,null,"Media historis hilang",missingSteps);
        var missingSaved=history.save(missing);ok(history.detail(missingSaved.id()).snapshot().equals(missing),"missing-media reference preserves stored historical information");
        DatabaseBootstrap.initialize();ok(history.detail(saved.id()).snapshot().equals(snapshot),"repeated migration preserves already stored session and steps");
        var retiring=login("alpha@example.test");var retiredAccess=new SessionAccess(retiring);var retiredSnapshot=result(null,null);
        PenggunaRepository.ConnectionFactory retireOnCommit=()->{
            Connection real=DatabaseConfig.getConnection();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,values)->{
                Object value=invoke(real,method,values);if(method.getName().equals("commit"))retiring.logout();return value;
            });
        };
        try{new TrainingHistory(retiredAccess,retireOnCommit).save(retiredSnapshot);throw new AssertionError();}
        catch(SecurityException expected){ok(true,"late commit delivery cannot update logged-out session");}
        ok(history.status(retiredSnapshot.operationId()).isPresent(),"commit for original owner remains durable despite rejected late callback");
        new AuthService(new PenggunaRepository(),a).logout();
        try{history.detail(saved.id());throw new AssertionError();}catch(SecurityException expected){ok(true,"old session read callback rejected after logout");}
        try{history.save(snapshot);throw new AssertionError();}catch(SecurityException expected){ok(true,"old session save callback rejected after logout");}
        a=login("alpha@example.test");var reopened=new TrainingHistory(new SessionAccess(a));ok(reopened.detail(saved.id()).snapshot().equals(snapshot),"relogin reads exact persisted snapshot");
        System.out.println("ALL "+checks+" JDBC CHECKS PASSED on "+db);
    }
}
