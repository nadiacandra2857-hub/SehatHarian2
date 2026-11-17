package com.sehatharian.training;

import java.util.Objects;

/** Complete immutable draft/engine state; a summary preserves the exact result snapshot for retry. */
public record TrainingCheckpoint(TrainingSession.State state,Integer programId,Integer programVersion,
        String programName,TrainingSnapshot result) {
    public TrainingCheckpoint {
        Objects.requireNonNull(state);
        if(programName==null||programName.isBlank()||programName.length()>255
                ||(programId==null)!=(programVersion==null)||programId!=null&&(programId<1||programVersion<1))throw invalid();
        if(state.phase()==TrainingSession.Phase.SUMMARY){
            if(result==null||!result.operationId().equals(state.operationId())||!result.startedAt().equals(state.startedAt())
                    ||!result.endedAt().equals(state.endedAt())||result.activeNanos()!=state.activeNanos()||result.stopped()!=state.stopped()
                    ||!Objects.equals(result.programId(),programId)||!Objects.equals(result.programVersion(),programVersion)
                    ||!result.programName().equals(programName)||result.steps().size()!=state.steps().size())throw invalid();
            for(int i=0;i<state.steps().size();i++){
                var a=state.steps().get(i);var b=result.steps().get(i);
                if(!a.exercise().externalId().equals(b.movementRef())||!a.exercise().name().equals(b.name())
                        ||a.repetitions()!=b.repetitions()||a.target()!=b.target()||state.stepActive().get(i)!=b.activeNanos()
                        ||state.outcomes().get(i)!=b.outcome()||!Objects.equals(state.confirmations().get(i),b.repetitionConfirmed())
                        ||b.confirmedRepetitions()!=null)throw invalid();
            }
        }else if(result!=null)throw invalid();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Snapshot checkpoint tidak sesuai keadaan sesi.");}
    public static TrainingCheckpoint capture(TrainingSession engine,Integer program,Integer version,String name,TrainingSnapshot result){
        return new TrainingCheckpoint(engine.exportState(),program,version,name,result);
    }
}
