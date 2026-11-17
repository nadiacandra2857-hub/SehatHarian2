package com.sehatharian;
import com.sehatharian.catalog.Exercise;
import com.sehatharian.training.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
class TrainingSessionTest {
    Exercise exercise=new Exercise("test","Test","","beginner","none","core","strength",List.of("Step"),List.of(),List.of(),List.of(),Map.of());
    AtomicLong time=new AtomicLong();
    TrainingSession session(boolean reps){return new TrainingSession(List.of(new TrainingSession.Step(exercise,reps,3),new TrainingSession.Step(exercise,false,3)),1,1,time::get);}
    void seconds(TrainingSession s,int n){for(int i=0;i<n;i++){time.addAndGet(1_000_000_000L);s.pulse(s.revision());}}
    void start(TrainingSession s){s.start();seconds(s,1);s.advance(s.revision(),false);}
    @Test void excludesPreparationRestPauseAndRequiresConfirmation(){var s=session(false);start(s);seconds(s,1);s.pause();seconds(s,10);s.resume();seconds(s,2);assertEquals(3,s.activeSeconds());assertEquals(0,s.completed());assertEquals(TrainingSession.Phase.CONFIRM,s.phase());s.advance(s.revision(),false);seconds(s,1);assertEquals(3,s.activeSeconds());assertEquals(1,s.completed());s.advance(s.revision(),false);seconds(s,3);s.advance(s.revision(),false);assertEquals(6,s.activeSeconds());assertEquals(TrainingSession.Phase.SUMMARY,s.phase());}
    @Test void staleAndDoubleCallbacksCannotAdvance(){var s=session(false);start(s);long revision=s.revision();s.advance(revision,true);s.advance(revision,true);assertEquals(1,s.skipped());assertEquals(1,s.index());s.pulse(revision);assertEquals(TrainingSession.Phase.REST,s.phase());}
    @Test void repetitionsAreManualAndInstancesIndependent(){var a=session(true);var b=session(false);start(a);seconds(a,5);assertEquals(TrainingSession.Phase.EXERCISE,a.phase());assertEquals(0,a.completed());assertEquals(0,b.activeSeconds());a.advance(a.revision(),false);assertEquals(1,a.completed());assertEquals(5,a.activeSeconds());}
    @Test void delayedHeartbeatPausesWithoutCreditingGap(){var s=session(false);start(s);seconds(s,1);time.addAndGet(30_000_000_000L);s.pulse(s.revision());assertTrue(s.paused());assertEquals(1,s.activeSeconds());assertEquals(0,s.completed());s.resume();seconds(s,1);s.stop();assertTrue(s.stopped());assertEquals(2,s.activeSeconds());seconds(s,10);assertEquals(2,s.activeSeconds());}
    @Test void missingBrowserAcknowledgementDoesNotCreditDetectionDelay(){var s=session(false);start(s);seconds(s,1);time.addAndGet(1_500_000_000L);s.suspend();assertEquals(1,s.activeSeconds());s.resume();seconds(s,1);assertEquals(2,s.activeSeconds());}
    @Test void fractionalCallbacksUseElapsedTime(){var s=session(false);start(s);for(int i=0;i<4;i++){time.addAndGet(250_000_000L);s.pulse(s.revision());}assertEquals(1,s.activeSeconds());assertEquals(2,s.remainingSeconds());}
    @Test void invalidConfigurationRejected(){assertThrows(IllegalArgumentException.class,()->new TrainingSession(List.of(),0,0,time::get));}
    @Test void snapshotKeepsExactStepNanosAndUnknownRepetitionCount(){
        var s=session(true);start(s);time.addAndGet(750_000_000L);s.pulse(s.revision());s.advance(s.revision(),false);
        seconds(s,1);s.advance(s.revision(),false);time.addAndGet(500_000_000L);s.pulse(s.revision());s.advance(s.revision(),true);
        var result=com.sehatharian.training.TrainingSnapshot.capture(s,null,null,"Susunan sendiri");
        assertEquals(1_250_000_000L,result.activeNanos());assertEquals(1,result.activeSeconds());
        assertEquals(result.activeNanos(),result.steps().stream().mapToLong(com.sehatharian.training.TrainingSnapshot.Step::activeNanos).sum());
        assertEquals(Boolean.TRUE,result.steps().getFirst().repetitionConfirmed());assertNull(result.steps().getFirst().confirmedRepetitions());
        assertEquals(TrainingSession.Outcome.DILEWATI,result.steps().getLast().outcome());
        assertEquals(result,com.sehatharian.training.TrainingSnapshot.capture(s,null,null,"Susunan sendiri"));
    }
    @Test void stoppedSnapshotDoesNotInventCompletionOrRepetitions(){
        var s=session(true);start(s);seconds(s,2);s.stop();
        var result=com.sehatharian.training.TrainingSnapshot.capture(s,null,null,"Susunan sendiri");
        assertTrue(result.stopped());assertNull(result.steps().getFirst().repetitionConfirmed());assertNull(result.steps().getFirst().confirmedRepetitions());
        assertEquals(TrainingSession.Outcome.BELUM_SELESAI,result.steps().getFirst().outcome());
        assertEquals(0,result.steps().getLast().activeNanos());
    }
    @Test void snapshotRejectsMismatchedSessionTotalAndActiveCapture(){
        var s=session(false);assertThrows(IllegalStateException.class,()->com.sehatharian.training.TrainingSnapshot.capture(s,null,null,"test"));
        start(s);seconds(s,2);s.stop();var r=com.sehatharian.training.TrainingSnapshot.capture(s,null,null,"test");
        assertThrows(IllegalArgumentException.class,()->new com.sehatharian.training.TrainingSnapshot(r.operationId(),r.startedAt(),r.endedAt(),r.activeNanos()+1,r.stopped(),null,null,r.programName(),r.steps()));
    }

    @Test void restoredExerciseUsesNewClockAndExplicitResume(){
        var s=session(true);start(s);seconds(s,2);var state=s.exportState();
        var otherClock=new AtomicLong(9_000_000_000_000L);var restored=TrainingSession.restore(state,otherClock::get);
        assertTrue(restored.paused());assertEquals(s.operationId(),restored.operationId());
        otherClock.addAndGet(86_400_000_000_000L);restored.pulse(restored.revision());assertEquals(2,restored.activeSeconds());
        restored.advance(restored.revision(),false);assertEquals(0,restored.completed());
        restored.resume();otherClock.addAndGet(1_000_000_000L);restored.pulse(restored.revision());assertEquals(3,restored.activeSeconds());
    }
    @Test void preparationRestAndConfirmationPreserveRemainingTime(){
        var prep=session(false);prep.start();time.addAndGet(250_000_000L);prep.pulse(prep.revision());
        var restored=TrainingSession.restore(prep.exportState(),time::get);assertTrue(restored.paused());assertEquals(1,restored.remainingSeconds());
        restored.resume();time.addAndGet(750_000_000L);restored.pulse(restored.revision());assertEquals(TrainingSession.Phase.CONFIRM,restored.phase());
        var confirm=TrainingSession.restore(restored.exportState(),time::get);assertTrue(confirm.paused());assertEquals(0,confirm.remainingSeconds());
        confirm.advance(confirm.revision(),false);assertEquals(TrainingSession.Phase.CONFIRM,confirm.phase());
        confirm.resume();confirm.advance(confirm.revision(),false);seconds(confirm,3);confirm.advance(confirm.revision(),false);
        time.addAndGet(250_000_000L);confirm.pulse(confirm.revision());
        var rest=TrainingSession.restore(confirm.exportState(),time::get);assertEquals(TrainingSession.Phase.REST,rest.phase());assertEquals(1,rest.remainingSeconds());
        seconds(rest,40);assertEquals(3,rest.activeSeconds());assertEquals(1,rest.remainingSeconds());
        rest.resume();time.addAndGet(750_000_000L);rest.pulse(rest.revision());assertEquals(TrainingSession.Phase.CONFIRM,rest.phase());
    }
    @Test void emptyDraftAndSummaryRoundTripWithExactResult(){
        var draft=TrainingSession.draft(UUID.randomUUID(),List.of(),5,15,time::get);
        var checkpoint=new TrainingCheckpoint(draft.exportState(),null,null,"Susunan sendiri",null);
        var decoded=TrainingCheckpointCodec.decode(1,TrainingCheckpointCodec.encode(checkpoint));assertEquals(checkpoint,decoded);
        assertTrue(TrainingSession.restore(decoded.state(),time::get).paused());assertThrows(IllegalStateException.class,draft::start);
        var engine=session(true);start(engine);seconds(engine,2);engine.stop();var snapshot=TrainingSnapshot.capture(engine,12,3,"Program lama");
        var summary=new TrainingCheckpoint(engine.exportState(),12,3,"Program lama",snapshot);
        var recovered=TrainingCheckpointCodec.decode(1,TrainingCheckpointCodec.encode(summary));assertEquals(snapshot,recovered.result());
        var restored=TrainingSession.restore(recovered.state(),time::get);assertTrue(restored.paused());assertEquals(TrainingSession.Phase.SUMMARY,restored.phase());
        assertEquals(snapshot.fingerprint(),TrainingSnapshot.capture(restored,12,3,"Program lama").fingerprint());
    }
    @Test void corruptUnsupportedAndInconsistentCheckpointAreRejected(){
        var engine=session(true);start(engine);seconds(engine,1);var r=engine.exportState();
        var checkpoint=new TrainingCheckpoint(r,null,null,"Test",null);byte[] data=TrainingCheckpointCodec.encode(checkpoint);
        assertThrows(IllegalArgumentException.class,()->TrainingCheckpointCodec.decode(2,data));
        assertThrows(IllegalArgumentException.class,()->TrainingCheckpointCodec.decode(1,Arrays.copyOf(data,data.length-1)));
        data[0]=0;assertThrows(IllegalArgumentException.class,()->TrainingCheckpointCodec.decode(1,data));
        assertThrows(IllegalArgumentException.class,()->new TrainingSession.State(1,r.operationId(),r.steps(),r.preparation(),r.rest(),9,r.phase(),r.confirmedFrom(),r.elapsedNanos(),r.activeNanos(),r.stepActive(),r.outcomes(),r.confirmations(),r.paused(),r.stopped(),r.startedAt(),r.endedAt(),r.phaseRevision()));
        assertThrows(IllegalArgumentException.class,()->new TrainingSession.State(1,r.operationId(),r.steps(),r.preparation(),r.rest(),r.index(),r.phase(),r.confirmedFrom(),r.elapsedNanos(),r.activeNanos()+1,r.stepActive(),r.outcomes(),r.confirmations(),r.paused(),r.stopped(),r.startedAt(),r.endedAt(),r.phaseRevision()));
    }
}
