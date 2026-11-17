package com.sehatharian.training;

import com.sehatharian.catalog.TeamExercises;
import java.time.*;
import java.util.*;

/** Immutable engine result. Nanos are preserved so sum(step durations) equals the session exactly. */
public record TrainingSnapshot(UUID operationId, Instant startedAt, Instant endedAt,
        long activeNanos, boolean stopped, Integer programId, Integer programVersion,
        String programName, List<Step> steps) {
    public record Step(String movementRef, String name, String mediaRef, String sourceRef,
            String category, boolean repetitions, int target, long activeNanos,
            TrainingSession.Outcome outcome, Boolean repetitionConfirmed, Integer confirmedRepetitions) {
        public Step {
            Objects.requireNonNull(movementRef); Objects.requireNonNull(name);
            Objects.requireNonNull(mediaRef); Objects.requireNonNull(sourceRef); Objects.requireNonNull(category);
            Objects.requireNonNull(outcome);
            if(name.isBlank()||name.length()>255||movementRef.length()>255||category.length()>255||target<1||target>3600||activeNanos<0)
                throw new IllegalArgumentException("Rincian langkah tidak valid.");
            if(repetitionConfirmed!=null && (!repetitions||outcome!=TrainingSession.Outcome.SELESAI||!repetitionConfirmed)
                    ||confirmedRepetitions!=null&&(confirmedRepetitions<0||!Boolean.TRUE.equals(repetitionConfirmed)))
                throw new IllegalArgumentException("Konfirmasi repetisi tidak valid.");
        }
    }
    public TrainingSnapshot {
        Objects.requireNonNull(operationId);Objects.requireNonNull(startedAt);Objects.requireNonNull(endedAt);
        startedAt=startedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        endedAt=endedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        steps=List.copyOf(steps);
        if(programName==null||programName.isBlank()||programName.length()>255||steps.isEmpty()||steps.size()>20
                ||activeNanos<0||endedAt.isBefore(startedAt)||steps.stream().mapToLong(Step::activeNanos).sum()!=activeNanos
                ||(programId==null)!=(programVersion==null)||programId!=null&&(programId<1||programVersion<1))
            throw new IllegalArgumentException("Snapshot sesi tidak valid.");
    }
    public long activeSeconds(){return activeNanos/1_000_000_000L;}
    public int minutes(){return (int)(activeSeconds()/60);}
    public long completed(){return steps.stream().filter(s->s.outcome()==TrainingSession.Outcome.SELESAI).count();}
    public static TrainingSnapshot capture(TrainingSession engine,Integer programId,Integer version,String name){
        if(engine.phase()!=TrainingSession.Phase.SUMMARY)throw new IllegalStateException("Sesi belum selesai.");
        List<Step> rows=new ArrayList<>();
        for(int i=0;i<engine.steps().size();i++){
            var s=engine.steps().get(i);var e=s.exercise();var clip=TeamExercises.find(e.externalId());
            String media=clip.map(c->"/images/team/"+c.file().replace(".mp4",".jpg")).orElseGet(()->e.images().getOrDefault("main",e.images().values().stream().sorted().findFirst().orElse("")));
            rows.add(new Step(e.externalId(),e.name(),media,clip.map(c->"DatasetAktivitas/"+c.file()).orElse("https://repdb.co/ · "+e.externalId()),e.category(),s.repetitions(),s.target(),engine.stepActiveNanos(i),engine.outcomes().get(i),engine.repetitionConfirmed(i),null));
        }
        return new TrainingSnapshot(engine.operationId(),engine.startedAt(),engine.endedAt(),engine.activeNanos(),engine.stopped(),programId,version,name,rows);
    }
    /** Length-prefixed canonical representation; independent of list/map iteration or default timezone. */
    public String fingerprint(){
        try{
            var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
            for(String v:List.of(operationId.toString(),startedAt.toString(),endedAt.toString(),Long.toString(activeNanos),Boolean.toString(stopped),String.valueOf(programId),String.valueOf(programVersion),programName))out.writeUTF(v);
            for(var s:steps){for(String v:List.of(s.movementRef(),s.name(),s.mediaRef(),s.sourceRef(),s.category(),Boolean.toString(s.repetitions()),Integer.toString(s.target()),Long.toString(s.activeNanos()),s.outcome().name(),String.valueOf(s.repetitionConfirmed()),String.valueOf(s.confirmedRepetitions())))out.writeUTF(v);}
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
