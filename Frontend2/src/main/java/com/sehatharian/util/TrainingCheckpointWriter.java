package com.sehatharian.util;

import com.sehatharian.training.*;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** At most one drain queued; offers replace pending state. The supplied executor is serial. */
public final class TrainingCheckpointWriter implements AutoCloseable {
    @FunctionalInterface public interface Store { TrainingCheckpoints.Entry write(long revision,TrainingCheckpoint checkpoint)throws SQLException; }
    private record Pending(long revision,TrainingCheckpoint checkpoint){}
    private final Store store;
    private final Executor executor;
    private final Consumer<Runnable> defer;
    private final Consumer<TrainingCheckpoints.Entry> confirmed;
    private final Consumer<Exception> failed;
    private final AtomicReference<Pending> pending=new AtomicReference<>();
    private final AtomicBoolean scheduled=new AtomicBoolean();
    private final AtomicLong revision;
    private volatile boolean closed;
    public TrainingCheckpointWriter(long revision,Store store,Executor executor,Consumer<Runnable> defer,
            Consumer<TrainingCheckpoints.Entry> confirmed,Consumer<Exception> failed){
        this.revision=new AtomicLong(revision);this.store=store;this.executor=executor;this.defer=defer;this.confirmed=confirmed;this.failed=failed;
    }
    public void offer(TrainingCheckpoint checkpoint){
        if(closed)return;
        pending.set(new Pending(revision.incrementAndGet(),checkpoint));schedule();
    }
    private void schedule(){if(!closed&&scheduled.compareAndSet(false,true))executor.execute(this::drain);}
    private void drain(){
        try{
            Pending next;while(!closed&&(next=pending.getAndSet(null))!=null){
                try{publish(store.write(next.revision(),next.checkpoint()));}
                catch(Exception error){if(!closed)defer.accept(()->{if(!closed)failed.accept(error);});}
            }
        }finally{scheduled.set(false);if(!closed&&pending.get()!=null)schedule();}
    }
    /** Run only on the same serial executor. Result saving waits for this confirmed summary checkpoint. */
    public TrainingCheckpoints.Entry flush(TrainingCheckpoint checkpoint)throws SQLException{
        if(closed)throw new TrainingCheckpoints.FencedException();
        pending.set(null);
        var entry=store.write(revision.incrementAndGet(),checkpoint);publish(entry);return entry;
    }
    private void publish(TrainingCheckpoints.Entry entry){if(!closed)defer.accept(()->{if(!closed)confirmed.accept(entry);});}
    public void cancelPending(){pending.set(null);}
    @Override public void close(){closed=true;pending.set(null);}
}
