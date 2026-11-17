package com.sehatharian.training;

import com.sehatharian.catalog.Exercise;
import java.util.*;
import java.util.function.LongSupplier;

/** Instance-owned state machine. Monotonic nanoseconds; callbacks carry a phase revision. */
public final class TrainingSession {
    public enum Phase { READY, PREPARATION, EXERCISE, REST, CONFIRM, SUMMARY }
    public record Step(Exercise exercise, boolean repetitions, int target) {
        public Step { Objects.requireNonNull(exercise); if(target<1 || target>3600)throw new IllegalArgumentException("Target 1–3600."); }
    }
    public enum Outcome { BELUM_SELESAI, SELESAI, DILEWATI }
    private final List<Outcome> outcomes;
    private final long[] stepActive;
    private final Boolean[] repetitionConfirmed;
    private final UUID operationId;
    private java.time.Instant startedAt, endedAt;
    public UUID operationId(){return operationId;}
    public long activeNanos(){return active;}
    public long stepActiveNanos(int position){return stepActive[position];}
    public Boolean repetitionConfirmed(int position){return repetitionConfirmed[position];}
    public java.time.Instant startedAt(){return startedAt;}
    public java.time.Instant endedAt(){return endedAt;}
    public List<Outcome> outcomes(){return List.copyOf(outcomes);}
    private final List<Step> steps;
    private final LongSupplier clock;
    private final int preparation, rest;
    private Phase phase=Phase.READY;
    private int index, completed, skipped;
    private long revision, last, elapsed, active;
    private boolean paused, stopped;
    public TrainingSession(List<Step> steps,int preparation,int rest,LongSupplier clock) {
        this(steps,preparation,rest,clock,UUID.randomUUID(),false);
    }
    private TrainingSession(List<Step> steps,int preparation,int rest,LongSupplier clock,UUID operation,boolean draft) {
        this.operationId=Objects.requireNonNull(operation);
        if((steps.isEmpty()&&!draft) || steps.size()>20 || preparation<0 || preparation>300 || rest<0 || rest>600)throw new IllegalArgumentException("Pengaturan sesi tidak valid.");
        this.steps=List.copyOf(steps);this.outcomes=new ArrayList<>(Collections.nCopies(steps.size(),Outcome.BELUM_SELESAI));this.preparation=preparation;this.rest=rest;this.clock=Objects.requireNonNull(clock);
        this.stepActive=new long[steps.size()];this.repetitionConfirmed=new Boolean[steps.size()];
    }
    public List<Step> steps(){return steps;} public Step current(){return steps.get(index);}
    public int index(){return index;} public Phase phase(){return phase;} public long revision(){return revision;}
    public boolean paused(){return paused;} public boolean stopped(){return stopped;}
    public int completed(){return completed;} public int skipped(){return skipped;}
    public long activeSeconds(){return active/1_000_000_000L;}
    private long limit(){return (long)(phase==Phase.PREPARATION?preparation:phase==Phase.REST?rest:current().target())*1_000_000_000L;}
    public long remainingSeconds(){if(phase==Phase.READY)return preparation;if(phase==Phase.CONFIRM||phase==Phase.SUMMARY)return 0;return Math.max(0,(limit()-elapsed+999_999_999L)/1_000_000_000L);}
    public long elapsedSeconds(){return elapsed/1_000_000_000L;}
    private void enter(Phase next){phase=next;elapsed=0;last=clock.getAsLong();revision++;paused=false;if(next==Phase.SUMMARY)endedAt=java.time.Instant.now();}
    public void start(){if(phase==Phase.READY){if(steps.isEmpty())throw new IllegalStateException("Pilih gerakan terlebih dahulu.");startedAt=java.time.Instant.now();enter(Phase.PREPARATION);}}
    /** A delayed/missing browser heartbeat pauses conservatively; no unobserved gap is credited. */
    public void tick(long expected) {
        if(expected!=revision || paused || phase==Phase.READY || phase==Phase.SUMMARY || phase==Phase.CONFIRM)return;
        long now=clock.getAsLong(), delta=Math.max(0,now-last);last=now;
        if(delta>2_000_000_000L){paused=true;revision++;return;}
        boolean timed=phase!=Phase.EXERCISE || !current().repetitions();
        long credited=timed?Math.min(delta,Math.max(0,limit()-elapsed)):delta;
        elapsed+=credited;if(phase==Phase.EXERCISE){active+=credited;stepActive[index]+=credited;}
        // Timer expiry asks for explicit confirmation, including after reconnect.
        if(timed && elapsed>=limit()) { confirmedFrom=phase;phase=Phase.CONFIRM;revision++; }
    }
    private Phase confirmedFrom=Phase.PREPARATION;
    public void pulse(long expected){Phase before=phase;tick(expected);if(before!=Phase.CONFIRM && phase==Phase.CONFIRM)confirmedFrom=before;}
    public void pause(){if(!paused && phase!=Phase.SUMMARY){pulse(revision);paused=true;revision++;}}
    /** Suspend at the last acknowledged sample, without crediting an unobserved gap. */
    public void suspend(){if(!paused && phase!=Phase.SUMMARY){paused=true;revision++;}}
    public void resume(){if(paused&&phase!=Phase.READY&&phase!=Phase.SUMMARY){paused=false;last=clock.getAsLong();revision++;}}
    public void advance(long expected,boolean skip) {
        if(expected!=revision || paused || phase==Phase.READY || phase==Phase.SUMMARY)return;
        Phase from=phase==Phase.CONFIRM?confirmedFrom:phase;
        if(from==Phase.PREPARATION || from==Phase.REST){enter(Phase.EXERCISE);return;}
        pulse(expected);
        if(paused)return;
        outcomes.set(index,skip?Outcome.DILEWATI:Outcome.SELESAI);
        if(current().repetitions()&&!skip)repetitionConfirmed[index]=true;
        if(skip)skipped++;else completed++;
        if(index+1==steps.size())enter(Phase.SUMMARY);else{index++;enter(Phase.REST);}
    }
    public void stop(){if(phase==Phase.SUMMARY||phase==Phase.READY)return;pulse(revision);stopped=true;enter(Phase.SUMMARY);}

    /** Persist accumulated values only. No nanoTime baseline is exported or trusted on import. */
    public record State(int format,UUID operationId,List<Step> steps,int preparation,int rest,
            int index,Phase phase,Phase confirmedFrom,long elapsedNanos,long activeNanos,
            List<Long> stepActive,List<Outcome> outcomes,List<Boolean> confirmations,
            boolean paused,boolean stopped,java.time.Instant startedAt,java.time.Instant endedAt,long phaseRevision) {
        public State {
            Objects.requireNonNull(operationId);Objects.requireNonNull(phase);Objects.requireNonNull(confirmedFrom);
            steps=List.copyOf(steps);stepActive=List.copyOf(stepActive);outcomes=List.copyOf(outcomes);
            confirmations=Collections.unmodifiableList(new ArrayList<>(confirmations));
            if(startedAt!=null)startedAt=startedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            if(endedAt!=null)endedAt=endedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            int n=steps.size();
            if(format!=1||n>20||preparation<0||preparation>300||rest<0||rest>600||index<0||index>=Math.max(1,n)
                    ||elapsedNanos<0||activeNanos<0||phaseRevision<0||phaseRevision==Long.MAX_VALUE
                    ||stepActive.size()!=n||outcomes.size()!=n||confirmations.size()!=n)
                throw invalid();
            long total=0;
            for(int i=0;i<n;i++){
                long duration=stepActive.get(i);var outcome=outcomes.get(i);var step=steps.get(i);Boolean confirmed=confirmations.get(i);
                if(duration<0||!step.repetitions()&&duration>step.target()*1_000_000_000L
                        ||i<index&&outcome==Outcome.BELUM_SELESAI
                        ||i>index&&(outcome!=Outcome.BELUM_SELESAI||duration!=0)
                        ||confirmed!=null&&(!confirmed||!step.repetitions()||outcome!=Outcome.SELESAI))throw invalid();
                try{total=Math.addExact(total,duration);}catch(ArithmeticException overflow){throw invalid();}
            }
            if(total!=activeNanos)throw invalid();
            if(phase==Phase.READY){
                if(index!=0||activeNanos!=0||elapsedNanos!=0||stopped||startedAt!=null||endedAt!=null||outcomes.stream().anyMatch(o->o!=Outcome.BELUM_SELESAI))throw invalid();
            }else{
                if(n==0||startedAt==null||phase!=Phase.SUMMARY&&(endedAt!=null||stopped||outcomes.get(index)!=Outcome.BELUM_SELESAI))throw invalid();
                if(phase==Phase.SUMMARY){
                    if(endedAt==null||endedAt.isBefore(startedAt)||elapsedNanos!=0
                            ||!stopped&&(index!=n-1||outcomes.contains(Outcome.BELUM_SELESAI)))throw invalid();
                }else{
                    Phase effective=phase==Phase.CONFIRM?confirmedFrom:phase;
                    if(!Set.of(Phase.PREPARATION,Phase.REST,Phase.EXERCISE).contains(effective))throw invalid();
                    long limit=(long)(effective==Phase.PREPARATION?preparation:effective==Phase.REST?rest:steps.get(index).target())*1_000_000_000L;
                    boolean timed=effective!=Phase.EXERCISE||!steps.get(index).repetitions();
                    if(timed&&elapsedNanos>limit||phase==Phase.CONFIRM&&(!timed||elapsedNanos!=limit)
                            ||effective==Phase.EXERCISE&&elapsedNanos!=stepActive.get(index)
                            ||effective==Phase.PREPARATION&&(index!=0||activeNanos!=0)
                            ||effective==Phase.REST&&(index==0||stepActive.get(index)!=0))throw invalid();
                }
            }
        }
        private static IllegalArgumentException invalid(){return new IllegalArgumentException("Checkpoint latihan tidak valid atau versinya belum didukung.");}
    }
    public State exportState(){
        List<Long> durations=new ArrayList<>();for(long duration:stepActive)durations.add(duration);
        return new State(1,operationId,steps,preparation,rest,index,phase,confirmedFrom,elapsed,active,durations,outcomes,Arrays.asList(repetitionConfirmed),paused,stopped,startedAt,endedAt,revision);
    }
    public static TrainingSession draft(UUID operation,List<Step> steps,int preparation,int rest,LongSupplier clock){
        return new TrainingSession(steps,preparation,rest,clock,operation,true);
    }
    /** Restored phases always wait for an explicit resume/start; closed time earns no credit. */
    public static TrainingSession restore(State state,LongSupplier clock){
        Objects.requireNonNull(state);
        var result=new TrainingSession(state.steps(),state.preparation(),state.rest(),clock,state.operationId(),true);
        result.index=state.index();result.phase=state.phase();result.confirmedFrom=state.confirmedFrom();
        result.elapsed=state.elapsedNanos();result.active=state.activeNanos();result.revision=state.phaseRevision()+1;
        result.outcomes.clear();result.outcomes.addAll(state.outcomes());
        for(int i=0;i<state.steps().size();i++){result.stepActive[i]=state.stepActive().get(i);result.repetitionConfirmed[i]=state.confirmations().get(i);}
        result.completed=(int)state.outcomes().stream().filter(o->o==Outcome.SELESAI).count();
        result.skipped=(int)state.outcomes().stream().filter(o->o==Outcome.DILEWATI).count();
        result.startedAt=state.startedAt();result.endedAt=state.endedAt();result.stopped=state.stopped();
        result.paused=true;result.last=clock.getAsLong();return result;
    }
}
