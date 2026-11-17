package com.sehatharian.training;

import com.sehatharian.catalog.Exercise;
import java.io.*;
import java.time.Instant;
import java.util.*;

/** Bounded, explicit binary format, never Java object deserialization. No process-local clock values. */
public final class TrainingCheckpointCodec {
    public static final int VERSION=1,MAX_BYTES=262144;
    private static final int MAGIC=0x53485443;
    private TrainingCheckpointCodec(){}
    public static byte[] encode(TrainingCheckpoint checkpoint){
        try{
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);var s=checkpoint.state();
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeUTF(s.operationId().toString());
            out.writeInt(checkpoint.programId()==null?0:checkpoint.programId());out.writeInt(checkpoint.programVersion()==null?0:checkpoint.programVersion());out.writeUTF(checkpoint.programName());
            out.writeInt(s.preparation());out.writeInt(s.rest());out.writeInt(s.index());out.writeUTF(s.phase().name());out.writeUTF(s.confirmedFrom().name());
            out.writeLong(s.elapsedNanos());out.writeLong(s.activeNanos());out.writeBoolean(s.paused());out.writeBoolean(s.stopped());instant(out,s.startedAt());instant(out,s.endedAt());out.writeLong(s.phaseRevision());
            out.writeInt(s.steps().size());
            for(int i=0;i<s.steps().size();i++){
                var step=s.steps().get(i);var e=step.exercise();
                for(String value:List.of(e.externalId(),e.name(),e.description(),e.difficulty(),e.equipment(),e.bodyPart(),e.category()))out.writeUTF(value);
                strings(out,e.instructions());strings(out,e.tips());strings(out,e.primaryMuscles());strings(out,e.secondaryMuscles());
                if(e.images().size()>32)throw invalid();out.writeInt(e.images().size());for(var key:new TreeSet<>(e.images().keySet())){out.writeUTF(key);out.writeUTF(e.images().get(key));}
                out.writeBoolean(step.repetitions());out.writeInt(step.target());out.writeLong(s.stepActive().get(i));out.writeUTF(s.outcomes().get(i).name());
                out.writeByte(s.confirmations().get(i)==null?-1:s.confirmations().get(i)?1:0);
            }
            out.writeBoolean(checkpoint.result()!=null);
            if(checkpoint.result()!=null)for(var step:checkpoint.result().steps()){
                out.writeUTF(step.mediaRef());out.writeUTF(step.sourceRef());out.writeUTF(step.category());
            }
            out.flush();if(bytes.size()>MAX_BYTES)throw invalid();return bytes.toByteArray();
        }catch(IOException|NullPointerException e){throw new IllegalArgumentException("Checkpoint belum dapat dikodekan.",e);}
    }
    public static TrainingCheckpoint decode(int version,byte[] bytes){
        if(version!=VERSION||bytes==null||bytes.length>MAX_BYTES)throw invalid();
        try{
            var in=new DataInputStream(new ByteArrayInputStream(bytes));
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw invalid();UUID operation=UUID.fromString(in.readUTF());
            int program=in.readInt(),revision=in.readInt();if(program<0||revision<0||(program==0)!=(revision==0))throw invalid();String name=in.readUTF();
            int preparation=in.readInt(),rest=in.readInt(),index=in.readInt();var phase=TrainingSession.Phase.valueOf(in.readUTF());var from=TrainingSession.Phase.valueOf(in.readUTF());
            long elapsed=in.readLong(),active=in.readLong();boolean paused=in.readBoolean(),stopped=in.readBoolean();Instant started=instant(in),ended=instant(in);long phaseRevision=in.readLong();
            int count=count(in,20);List<TrainingSession.Step> steps=new ArrayList<>();List<Long> durations=new ArrayList<>();List<TrainingSession.Outcome> outcomes=new ArrayList<>();List<Boolean> confirmations=new ArrayList<>();
            for(int i=0;i<count;i++){
                String id=in.readUTF(),title=in.readUTF(),description=in.readUTF(),difficulty=in.readUTF(),equipment=in.readUTF(),body=in.readUTF(),category=in.readUTF();
                List<String> instructions=strings(in),tips=strings(in),primary=strings(in),secondary=strings(in);Map<String,String> images=new HashMap<>();
                int imageCount=count(in,32);for(int j=0;j<imageCount;j++){String key=in.readUTF();if(images.put(key,in.readUTF())!=null)throw invalid();}
                var exercise=new Exercise(id,title,description,difficulty,equipment,body,category,instructions,tips,primary,secondary,images);
                steps.add(new TrainingSession.Step(exercise,in.readBoolean(),in.readInt()));durations.add(in.readLong());outcomes.add(TrainingSession.Outcome.valueOf(in.readUTF()));
                int confirmation=in.readByte();if(confirmation< -1||confirmation>1)throw invalid();confirmations.add(confirmation==-1?null:confirmation==1);
            }
            var state=new TrainingSession.State(VERSION,operation,steps,preparation,rest,index,phase,from,elapsed,active,durations,outcomes,confirmations,paused,stopped,started,ended,phaseRevision);
            TrainingSnapshot snapshot=null;
            if(in.readBoolean()){
                List<TrainingSnapshot.Step> result=new ArrayList<>();
                for(int i=0;i<count;i++){var step=steps.get(i);result.add(new TrainingSnapshot.Step(step.exercise().externalId(),step.exercise().name(),in.readUTF(),in.readUTF(),in.readUTF(),step.repetitions(),step.target(),durations.get(i),outcomes.get(i),confirmations.get(i),null));}
                snapshot=new TrainingSnapshot(operation,started,ended,active,stopped,program==0?null:program,revision==0?null:revision,name,result);
            }
            if(in.available()!=0)throw invalid();return new TrainingCheckpoint(state,program==0?null:program,revision==0?null:revision,name,snapshot);
        }catch(IOException|RuntimeException e){throw new IllegalArgumentException("Checkpoint rusak atau versinya belum didukung.",e);}
    }
    private static void strings(DataOutputStream out,List<String> values)throws IOException{if(values.size()>128)throw invalid();out.writeInt(values.size());for(String value:values)out.writeUTF(value);}
    private static List<String> strings(DataInputStream in)throws IOException{int count=count(in,128);List<String> values=new ArrayList<>();for(int i=0;i<count;i++)values.add(in.readUTF());return values;}
    private static int count(DataInputStream in,int maximum)throws IOException{int count=in.readInt();if(count<0||count>maximum)throw invalid();return count;}
    private static void instant(DataOutputStream out,Instant instant)throws IOException{out.writeBoolean(instant!=null);if(instant!=null)out.writeLong(instant.toEpochMilli());}
    private static Instant instant(DataInputStream in)throws IOException{return in.readBoolean()?Instant.ofEpochMilli(in.readLong()):null;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Checkpoint tidak valid atau versinya belum didukung.");}
    public static String hash(byte[] bytes){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
