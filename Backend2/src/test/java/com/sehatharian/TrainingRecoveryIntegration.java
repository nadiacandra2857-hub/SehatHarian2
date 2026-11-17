package com.sehatharian;

import com.sehatharian.catalog.*;
import com.sehatharian.config.*;
import com.sehatharian.content.ContentService;
import com.sehatharian.content.ContentService.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.storage.*;
import com.sehatharian.training.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit synthetic schema only; restart assertions run in a separate JVM. */
public final class TrainingRecoveryIntegration {
    static int checks;
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+(++checks)+" "+message);}
    interface Checked {void run()throws Exception;}
    static void rejected(Checked run,String message)throws Exception{try{run.run();}catch(SecurityException|IllegalStateException|IllegalArgumentException expected){ok(true,message);return;}throw new AssertionError(message);}
    static long scalar(String sql)throws Exception{try(var c=DatabaseConfig.getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static void sql(String sql)throws Exception{try(var c=DatabaseConfig.getConnection();var s=c.createStatement()){s.executeUpdate(sql);}}
    static UserSession login(String email)throws Exception{var session=new UserSession();new AuthService(new PenggunaRepository(),session).login(email,"fixture123");return session;}
    static void account(String email)throws Exception{new AuthService(new PenggunaRepository(),new UserSession()).register("Recovery Fixture",email,"fixture123","fixture123");}
    static void tick(TrainingSession engine,AtomicLong clock,int seconds){for(int i=0;i<seconds;i++){clock.addAndGet(1_000_000_000L);engine.pulse(engine.revision());}}
    static TrainingCheckpoint checkpoint(TrainingSession engine){return TrainingCheckpoint.capture(engine,null,null,"Program pemulihan fixture",engine.phase()==TrainingSession.Phase.SUMMARY?TrainingSnapshot.capture(engine,null,null,"Program pemulihan fixture"):null);}
    static TrainingSession engine(UUID operation,AtomicLong clock){var exercise=TeamExercises.ALL.get(0).exercise();return TrainingSession.draft(operation,List.of(new TrainingSession.Step(exercise,false,60),new TrainingSession.Step(exercise,true,8)),5,4,clock::get);}
    static TrainingSession at(String phase){
        var clock=new AtomicLong();var engine=engine(UUID.randomUUID(),clock);
        if(phase.equals("READY"))return engine;
        engine.start();if(phase.equals("PREPARATION")){tick(engine,clock,2);return engine;}
        tick(engine,clock,5);if(phase.equals("CONFIRM"))return engine;
        engine.advance(engine.revision(),false);tick(engine,clock,10);
        if(phase.equals("EXERCISE"))return engine;
        tick(engine,clock,50);engine.advance(engine.revision(),false);clock.addAndGet(1_250_000_000L);engine.pulse(engine.revision());
        if(phase.equals("REST"))return engine;
        tick(engine,clock,2);clock.addAndGet(750_000_000L);engine.pulse(engine.revision());engine.advance(engine.revision(),false);tick(engine,clock,2);engine.advance(engine.revision(),false);return engine;
    }
    static Object invoke(Object target,Method method,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}}
    static PenggunaRepository.ConnectionFactory fault(String mode){return ()->{
        var real=DatabaseConfig.getConnection();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            if(method.getName().equals("commit")&&mode.equals("ack")){real.commit();throw new SQLException("fixture acknowledgement lost");}
            Object value=invoke(real,method,args);
            if(method.getName().equals("prepareStatement")){
                String query=args[0].toString();boolean fail=mode.equals("checkpoint")&&query.startsWith("UPDATE training_checkpoint SET revision=")||mode.equals("result")&&query.startsWith("INSERT INTO training_step")||mode.equals("receipt")&&query.startsWith("SELECT * FROM training_session");
                if(fail){var statement=(PreparedStatement)value;return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{if(m.getName().equals("executeUpdate")||m.getName().equals("executeQuery"))throw new SQLException("fixture statement failure");return invoke(statement,m,a);});}
            }return value;
        });
    };}
    static void freshProcessRead()throws Exception{
        for(String phase:List.of("READY","PREPARATION","EXERCISE","REST","CONFIRM","SUMMARY")){
            var entry=new TrainingCheckpoints(new SessionAccess(login(phase.toLowerCase()+"@example.test"))).inspect();
            ok(entry!=null&&entry.checkpoint()!=null&&entry.checkpoint().state().phase().name().equals(phase),"fresh JVM reads "+phase+" snapshot");
            var clock=new AtomicLong(7_000_000_000_000_000L);var engine=TrainingSession.restore(entry.checkpoint().state(),clock::get);long active=engine.activeNanos(),elapsed=engine.elapsedSeconds();
            tick(engine,clock,120);ok(engine.paused()&&active==engine.activeNanos()&&elapsed==engine.elapsedSeconds(),"fresh JVM "+phase+" stays paused; shutdown time earns no credit");
            if(phase.equals("EXERCISE")){engine.resume();tick(engine,clock,1);ok(engine.activeNanos()==active+1_000_000_000L,"explicit resume starts new monotonic baseline");}
            if(phase.equals("REST"))ok(engine.remainingSeconds()==3,"partial rest preserves remaining 2.75 seconds");
            if(phase.equals("SUMMARY"))ok(entry.checkpoint().result().fingerprint().equals(TrainingSnapshot.capture(engine,null,null,"Program pemulihan fixture").fingerprint()),"summary restores identical operation and result fingerprint");
        }
    }
    public static void main(String[] args)throws Exception{
        String name=System.getenv("DB_NAME");if(name==null||!name.matches("sehatharian_test_recovery_[a-z0-9_]+"))throw new SecurityException("Dedicated recovery test schema required");
        if(args.length>0&&args[0].equals("read")){freshProcessRead();System.out.println("ALL "+checks+" FRESH JVM CHECKS PASSED");return;}
        DatabaseBootstrap.initialize();DatabaseBootstrap.initialize();ok(scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('training_checkpoint','training_checkpoint_slot','training_session','training_step','aktivitas') AND engine='InnoDB'")==5,"actual checkpoint/history tables use InnoDB and migration repeats safely");
        account("alpha@example.test");account("beta@example.test");account("recovery@example.test");
        for(String phase:List.of("READY","PREPARATION","EXERCISE","REST","CONFIRM","SUMMARY")){
            String email=phase.toLowerCase()+"@example.test";account(email);var service=new TrainingCheckpoints(new SessionAccess(login(email)));var engine=at(phase);UUID token=UUID.randomUUID();
            service.create(token,checkpoint(TrainingSession.draft(engine.operationId(),engine.steps(),5,4,System::nanoTime)));service.write(engine.operationId(),token,2,checkpoint(engine));
        }
        sql("UPDATE pengguna SET onboarding_selesai=TRUE WHERE email LIKE '%@example.test'");
        try(var c=DatabaseConfig.getConnection();var p=c.prepareStatement("INSERT INTO pengguna(nama,email,password_hash,role,onboarding_selesai) VALUES('Recovery Admin','admin@example.test',?,'admin',TRUE)")){p.setString(1,com.sehatharian.util.PasswordUtil.hashPassword("fixture123"));p.executeUpdate();}
        var cms=new ContentService(new SessionAccess(login("admin@example.test")));int movement=cms.saveDraft(null,Kind.MOVEMENT,"Jalan tim","team-walking",List.of());cms.transition(movement,State.REVIEWED);cms.transition(movement,State.PUBLISHED);int program=cms.saveDraft(null,Kind.PROGRAM,"Program Pemulihan","Sesi 60 detik untuk penerimaan restart",List.of(new ContentService.Step(movement,60,false)));cms.transition(program,State.REVIEWED);cms.transition(program,State.PUBLISHED);
        var a=login("alpha@example.test");var access=new SessionAccess(a);var service=new TrainingCheckpoints(access);var b=login("beta@example.test");var other=new TrainingCheckpoints(new SessionAccess(b));UUID holder=UUID.randomUUID();var engine=at("EXERCISE");UUID operation=engine.operationId();var initial=checkpoint(TrainingSession.draft(operation,engine.steps(),5,4,System::nanoTime));
        var first=service.create(holder,initial);ok(first.revision()==1&&service.create(holder,initial).revision()==1,"creation is idempotent with stable operation and holder");
        var current=service.write(operation,holder,3,checkpoint(engine));ok(current.revision()==3&&current.checkpoint().state().activeNanos()==10_000_000_000L,"new revision stores engine progress");
        rejected(()->service.write(operation,holder,2,initial),"late checkpoint cannot overwrite newer revision");ok(service.inspect().revision()==3,"rejected older revision leaves newer payload unchanged");
        ok(other.inspect()==null,"other account cannot read owner checkpoint");rejected(()->other.claim(operation,holder,UUID.randomUUID()),"other account cannot take over owner checkpoint");rejected(()->other.write(operation,holder,100,checkpoint(engine)),"other account cannot modify owner checkpoint");
        UUID second=UUID.randomUUID();var claimed=service.claim(operation,holder,second);ok(claimed.revision()==4&&claimed.holder().equals(second),"explicit takeover rotates holder and increments revision");
        rejected(()->service.verify(operation,holder),"old instance loses authority after takeover");rejected(()->service.write(operation,holder,99,checkpoint(engine)),"old instance cannot write even with larger revision");
        var history=new TrainingHistory(access);var finished=at("SUMMARY");
        var summaryEngine=TrainingSession.restore(new TrainingSession.State(1,operation,finished.steps(),5,4,finished.index(),finished.phase(),finished.exportState().confirmedFrom(),finished.exportState().elapsedNanos(),finished.activeNanos(),finished.exportState().stepActive(),finished.outcomes(),finished.exportState().confirmations(),false,false,engine.startedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),java.time.Instant.now().plusMillis(1),finished.revision()),System::nanoTime);
        var summary=checkpoint(summaryEngine);service.write(operation,second,5,summary);
        rejected(()->history.save(summary.result(),holder),"old instance cannot commit training result after takeover");
        var failed=WriteResult.attempt(()->new TrainingCheckpoints(access,fault("checkpoint")).write(operation,second,6,summary));ok(failed.status()==WriteResult.Status.GAGAL&&service.inspect().revision()==5,"failed checkpoint rolls back; confirmed state is unchanged");
        var uncertain=WriteResult.attempt(()->new TrainingCheckpoints(access,fault("ack")).write(operation,second,6,summary));ok(uncertain.needsReview(),"lost checkpoint commit acknowledgement remains unconfirmed to caller");
        ok(service.inspect().revision()==6&&service.write(operation,second,6,summary).revision()==6,"probe and same-revision retry confirm exact checkpoint without duplicate operation");
        long activities=scalar("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id="+access.owner());
        var resultFailure=WriteResult.attempt(()->new TrainingHistory(access,fault("result")).save(summary.result(),second));ok(resultFailure.status()==WriteResult.Status.GAGAL,"pre-commit result fault confirms rollback");
        ok(scalar("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id="+access.owner())==activities&&service.inspect().status()==TrainingCheckpoints.Status.ACTIVE&&service.inspect().checkpoint().result().equals(summary.result()),"failed result leaves no partial activity; same summary stays recoverable");
        try{new TrainingCheckpoints(access,fault("receipt")).inspect();throw new AssertionError();}catch(SQLException expected){ok(true,"receipt query failure does not return a false not-committed recovery decision");}
        var resultUncertain=WriteResult.attempt(()->new TrainingHistory(access,fault("ack")).save(summary.result(),second));ok(resultUncertain.needsReview(),"result commit acknowledgement can be lost after durable commit");
        var completed=service.inspect();ok(completed.status()==TrainingCheckpoints.Status.COMPLETED&&completed.receipt()!=null&&completed.operation().equals(operation),"commit atomically marks checkpoint completed; restart resolves receipt without cleanup callback");
        ok(scalar("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id="+access.owner())==activities+1&&history.save(summary.result(),second).id()==completed.receipt().id(),"retry after commit/crash returns one activity and one history session");
        rejected(()->service.write(operation,second,100,summary),"late checkpoint cannot resurrect completed session");rejected(()->service.create(second,initial),"completed operation cannot be created as a new workout");
        var discardEngine=at("READY");UUID discardToken=UUID.randomUUID();service.create(discardToken,checkpoint(discardEngine));
        service.discard(discardEngine.operationId(),discardToken);rejected(()->service.write(discardEngine.operationId(),discardToken,100,checkpoint(discardEngine)),"late checkpoint rejected after explicit discard");rejected(()->service.create(UUID.randomUUID(),checkpoint(discardEngine)),"discard tombstone prevents operation resurrection");
        ok(scalar("SELECT COUNT(*) FROM training_checkpoint WHERE pengguna_id="+access.owner()+" AND status='DISCARDED' AND payload IS NULL")==1,"discard removes payload while retaining terminal operation marker");
        var bad=at("READY");UUID badHolder=UUID.randomUUID();service.create(badHolder,checkpoint(bad));sql("UPDATE training_checkpoint SET format_version=99 WHERE pengguna_id="+access.owner()+" AND status='ACTIVE'");ok(service.inspect().checkpoint()==null&&service.inspect().invalidReason()!=null,"unsupported version is reported, never replaced with an empty draft");service.discard(bad.operationId(),badHolder);
        var corrupt=at("READY");UUID corruptHolder=UUID.randomUUID();service.create(corruptHolder,checkpoint(corrupt));sql("UPDATE training_checkpoint SET payload=X'010203' WHERE pengguna_id="+access.owner()+" AND status='ACTIVE'");ok(service.inspect().checkpoint()==null&&service.inspect().invalidReason()!=null,"corrupt payload/hash exposes discard option without importing state");service.discard(corrupt.operationId(),corruptHolder);
        var race1=at("READY");var race2=at("READY");UUID token1=UUID.randomUUID(),token2=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);var go=new CountDownLatch(1);
        try{
            var f=pool.submit(()->{go.await();try{service.create(token1,checkpoint(race1));return true;}catch(TrainingCheckpoints.FencedException lost){return false;}});
            var g=pool.submit(()->{go.await();try{service.create(token2,checkpoint(race2));return true;}catch(TrainingCheckpoints.FencedException lost){return false;}});go.countDown();ok(f.get()!=g.get(),"two simultaneous instances create only one active slot");
        }finally{pool.shutdownNow();}
        var winner=service.inspect();ok(scalar("SELECT COUNT(*) FROM training_checkpoint WHERE pengguna_id="+access.owner()+" AND status='ACTIVE'")==1,"one recoverable operation per owner remains active");
        var takeovers=Executors.newFixedThreadPool(2);var takeGo=new CountDownLatch(1);UUID new1=UUID.randomUUID(),new2=UUID.randomUUID();
        try{
            var f=takeovers.submit(()->{takeGo.await();try{service.claim(winner.operation(),winner.holder(),new1);return true;}catch(TrainingCheckpoints.FencedException lost){return false;}});
            var g=takeovers.submit(()->{takeGo.await();try{service.claim(winner.operation(),winner.holder(),new2);return true;}catch(TrainingCheckpoints.FencedException lost){return false;}});takeGo.countDown();ok(f.get()!=g.get(),"simultaneous explicit takeovers grant exactly one new holder");
        }finally{takeovers.shutdownNow();}
        rejected(()->service.verify(winner.operation(),winner.holder()),"previous holder is fenced after concurrent takeover");var latest=service.inspect();service.discard(latest.operation(),latest.holder());
        var beforeLogout=at("READY");UUID logoutToken=UUID.randomUUID();service.create(logoutToken,checkpoint(beforeLogout));a.logout();
        rejected(service::inspect,"old session cannot read checkpoint after logout");rejected(()->service.write(beforeLogout.operationId(),logoutToken,10,checkpoint(beforeLogout)),"old session cannot deliver checkpoint after logout");
        var reloggedAccess=new SessionAccess(login("alpha@example.test"));var relogin=new TrainingCheckpoints(reloggedAccess);ok(relogin.inspect().operation().equals(beforeLogout.operationId()),"logout preserves last confirmed checkpoint for same owner after login");relogin.discard(beforeLogout.operationId(),logoutToken);
        sql("DELETE FROM training_checkpoint WHERE pengguna_id="+reloggedAccess.owner()+" AND operation_id='"+operation+"' AND status='COMPLETED'");
        rejected(()->relogin.create(UUID.randomUUID(),initial),"pre-checkpoint legacy receipt also prevents re-creating the same operation");
        System.out.println("ALL "+checks+" JDBC RECOVERY CHECKS PASSED on "+name);
    }
}
