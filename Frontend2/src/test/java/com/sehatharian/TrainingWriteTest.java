package com.sehatharian;
import com.sehatharian.catalog.Exercise;
import com.sehatharian.training.TrainingSession;
import com.sehatharian.storage.WriteResult;
import com.sehatharian.util.WriteAction;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
class TrainingWriteTest {
    TrainingSession completed(){
        var clock=new AtomicLong();var e=new Exercise("test","Test","","beginner","none","core","strength",List.of("Step"),List.of(),List.of(),List.of(),Map.of());
        var s=new TrainingSession(List.of(new TrainingSession.Step(e,true,10)),0,15,clock::get);s.start();s.pulse(s.revision());s.advance(s.revision(),false);
        for(int i=0;i<65;i++){clock.addAndGet(1_000_000_000L);s.pulse(s.revision());}s.advance(s.revision(),false);return s;
    }
    @Test void uncertainActivityKeepsSummaryAndRejectsRepeat() throws Exception {check(true);}
    @Test void failedActivityKeepsSummaryWithoutAutomaticRetry() throws Exception {check(false);}
    @Test void confirmedFailureAllowsExplicitResubmissionAfterRelease() throws Exception {
        var f=new FrontendOwnershipFixtures();var queue=new ArrayDeque<Runnable>();
        var gate=new WriteAction(f.access,queue::add);var result=new ArrayList<WriteResult<?>>();
        f.db.failAfterExecute=true;
        com.sehatharian.model.SessionAccess.Operation<WriteResult<Boolean>> command=()->WriteResult.attempt(()->{
            f.kebiasaan().tambahAktivitas(f.access.owner(),LocalDate.of(2026,11,4),"Latihan RepDB",1,"Aktif 65 detik");return true;});
        assertTrue(gate.execute(command,result::add,b->{}));
        assertEquals(WriteResult.Status.GAGAL,result.getFirst().status());
        assertFalse(gate.execute(command,result::add,b->{}));
        while(!queue.isEmpty())queue.remove().run();
        assertFalse(gate.reviewRequired());f.db.failAfterExecute=false;
        assertTrue(gate.execute(command,result::add,b->{}));
        assertTrue(result.getLast().confirmed());assertEquals(2,f.db.executions);
    }
    void check(boolean uncertain) throws Exception {
        var f=new FrontendOwnershipFixtures();var s=completed();var queue=new ArrayDeque<Runnable>();var gate=new WriteAction(f.access,queue::add);var result=new ArrayList<WriteResult<?>>();
        f.db.loseCommitAck=uncertain;f.db.failAfterExecute=!uncertain;
        com.sehatharian.model.SessionAccess.Operation<WriteResult<Boolean>> command=()->WriteResult.attempt(()->{f.kebiasaan().tambahAktivitas(f.access.owner(),LocalDate.of(2026,11,2),"Latihan RepDB",(int)(s.activeSeconds()/60),"Aktif "+s.activeSeconds()+" detik");return true;});
        assertTrue(gate.execute(command,result::add,b->{}));assertFalse(gate.execute(command,result::add,b->{}));while(!queue.isEmpty())queue.remove().run();
        assertEquals(65,s.activeSeconds());assertEquals(1,s.completed());assertEquals(1,f.db.executions);assertEquals(uncertain?WriteResult.Status.BELUM_PASTI:WriteResult.Status.GAGAL,result.getFirst().status());
        if(uncertain)assertFalse(gate.execute(command,result::add,b->{}));assertEquals(1,f.db.executions);
        f.loginB();assertThrows(SecurityException.class,()->gate.execute(command,result::add,b->{}));assertEquals(1,f.db.executions);
    }
    @Test void uncertainGateRequiresReviewAndRejectsOldIdentityAfterReview() throws Exception {
        var f=new FrontendOwnershipFixtures();var queue=new ArrayDeque<Runnable>();var gate=new WriteAction(f.access,queue::add);
        assertTrue(gate.execute(()->WriteResult.uncertain(),r->{},b->{}));
        assertThrows(IllegalStateException.class,gate::operationReviewed);
        while(!queue.isEmpty())queue.remove().run();assertTrue(gate.reviewRequired());
        gate.operationReviewed();assertFalse(gate.reviewRequired());
        f.loginB();assertThrows(SecurityException.class,gate::operationReviewed);
    }

    @Test void checkpointWriterCoalescesOffersAndDoesNotDeliverAfterClose(){
        var worker=new ArrayDeque<Runnable>();var ui=new ArrayDeque<Runnable>();var revisions=new ArrayList<Long>();var delivered=new ArrayList<Object>();
        var engine=completed();var result=com.sehatharian.training.TrainingSnapshot.capture(engine,null,null,"Test");
        var state=com.sehatharian.training.TrainingCheckpoint.capture(engine,null,null,"Test",result);
        var writer=new com.sehatharian.util.TrainingCheckpointWriter(10,(revision,value)->{revisions.add(revision);return null;},worker::add,ui::add,delivered::add,error->fail(error));
        writer.offer(state);writer.offer(state);writer.offer(state);assertEquals(1,worker.size());assertTrue(revisions.isEmpty());
        worker.remove().run();assertEquals(List.of(13L),revisions);assertTrue(delivered.isEmpty());
        writer.close();ui.remove().run();assertTrue(delivered.isEmpty());writer.offer(state);assertTrue(worker.isEmpty());
    }
    @Test void checkpointFlushSupersedesQueuedOlderState() throws Exception {
        var worker=new ArrayDeque<Runnable>();var revisions=new ArrayList<Long>();var engine=completed();
        var state=com.sehatharian.training.TrainingCheckpoint.capture(engine,null,null,"Test",com.sehatharian.training.TrainingSnapshot.capture(engine,null,null,"Test"));
        var writer=new com.sehatharian.util.TrainingCheckpointWriter(1,(revision,value)->{revisions.add(revision);return null;},worker::add,Runnable::run,entry->{},error->fail(error));
        writer.offer(state);writer.offer(state);writer.flush(state);while(!worker.isEmpty())worker.remove().run();
        assertEquals(List.of(4L),revisions);
    }
    @Test void asyncWriteRunsOffDispatcherAndRejectsLateIdentity() throws Exception {
        var f=new FrontendOwnershipFixtures();var worker=new ArrayDeque<Runnable>();var ui=new ArrayDeque<Runnable>();var received=new ArrayList<Object>();var busy=new ArrayList<Boolean>();
        var gate=new WriteAction(f.access,ui::add);var calls=new ArrayList<String>();
        assertTrue(gate.executeAsync(worker::add,()->{calls.add("worker");return WriteResult.success(true);},received::add,busy::add));
        assertTrue(calls.isEmpty());assertEquals(List.of(true),busy);assertFalse(gate.executeAsync(worker::add,()->WriteResult.success(true),received::add,busy::add));
        worker.remove().run();assertTrue(received.isEmpty());f.loginB();ui.remove().run();assertTrue(received.isEmpty());assertEquals(List.of(true),busy);assertFalse(gate.isRunning());
    }
}
