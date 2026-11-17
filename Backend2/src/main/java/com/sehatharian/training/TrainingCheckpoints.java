package com.sehatharian.training;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
import java.util.*;

/** One durable slot per verified owner, fenced by a token; terminal operation rows are never recycled. */
public final class TrainingCheckpoints extends OwnedRepository {
    public enum Status { ACTIVE, DISCARDED, COMPLETED }
    public static final class FencedException extends IllegalStateException {
        public FencedException(){super("Sesi latihan telah berpindah, dibuang, atau selesai. Periksa sesi tersimpan.");}
    }
    public record Entry(UUID operation,UUID holder,long revision,Status status,Instant checkpointAt,
            TrainingCheckpoint checkpoint,String invalidReason,TrainingHistory.Saved receipt) {}
    @FunctionalInterface private interface Work<T>{T run(Connection c,WriteTransaction tx)throws SQLException;}
    public TrainingCheckpoints(SessionAccess access){super(access);}
    public TrainingCheckpoints(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    private <T> T transaction(Work<T> work)throws SQLException {
        var tx=new WriteTransaction(access,connections);
        try{var c=tx.open();lockOwner(c,access);return tx.confirm(work.run(c,tx));}
        catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    static void lockOwner(Connection c,SessionAccess access)throws SQLException{
        try(var p=c.prepareStatement("SELECT id FROM pengguna WHERE id=? FOR UPDATE")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new SecurityException("Pemilik tidak tersedia.");}
        }access.owner();
    }
    private Entry row(Connection c,UUID operation)throws SQLException{
        try(var p=c.prepareStatement("SELECT * FROM training_checkpoint WHERE pengguna_id=? AND operation_id=?")){
            p.setInt(1,access.owner());p.setString(2,operation.toString());try(var r=p.executeQuery()){return r.next()?map(r,null):null;}
        }
    }
    private Entry current(Connection c)throws SQLException{
        try(var p=c.prepareStatement("SELECT t.* FROM training_checkpoint_slot s JOIN training_checkpoint t ON t.pengguna_id=s.pengguna_id AND t.operation_id=s.operation_id WHERE s.pengguna_id=?")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){return r.next()?map(r,null):null;}
        }
    }
    private Entry map(ResultSet r,TrainingHistory.Saved receipt)throws SQLException{
        access.owner(r.getInt("pengguna_id"));UUID operation=UUID.fromString(r.getString("operation_id"));String holder=r.getString("holder_token");
        TrainingCheckpoint checkpoint=null;String invalid=null;
        if(r.getString("status").equals("ACTIVE"))try{
            byte[] bytes;try(var input=r.getBinaryStream("payload")){bytes=input==null?null:input.readNBytes(TrainingCheckpointCodec.MAX_BYTES+1);}
            if(bytes==null||!TrainingCheckpointCodec.hash(bytes).equals(r.getString("payload_hash")))throw new IllegalArgumentException();
            checkpoint=TrainingCheckpointCodec.decode(r.getInt("format_version"),bytes);
            if(!checkpoint.state().operationId().equals(operation))throw new IllegalArgumentException();
        }catch(java.io.IOException|IllegalArgumentException corrupted){invalid="Checkpoint rusak atau versinya belum didukung. Progres lokal dan riwayat tersimpan tidak diubah.";}
        return new Entry(operation,holder==null?null:UUID.fromString(holder),r.getLong("revision"),Status.valueOf(r.getString("status")),r.getObject("checkpoint_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),checkpoint,invalid,receipt);
    }
    /** Receipt lookup shares the owner lock: no false absence while a result writer is committing. */
    public Entry inspect()throws SQLException{
        return transaction((c,tx)->reconcile(c,tx,current(c)));
    }
    private Entry reconcile(Connection c,WriteTransaction tx,Entry entry)throws SQLException{
        if(entry==null||entry.status()==Status.DISCARDED)return entry;
        var receipt=new TrainingHistory(access,connections).findOperation(c,entry.operation());
        if(receipt!=null){
            complete(c,tx,access,entry.operation());var stored=row(c,entry.operation());
            return new Entry(stored.operation(),stored.holder(),stored.revision(),stored.status(),stored.checkpointAt(),null,null,receipt);
        }
        if(entry.status()==Status.COMPLETED)throw new SQLException("Status hasil belum dapat dipastikan.");
        return entry;
    }
    public Entry create(UUID holder,TrainingCheckpoint initial)throws SQLException{
        if(initial.state().phase()!=TrainingSession.Phase.READY)throw new IllegalArgumentException("Susunan awal diperlukan.");
        Objects.requireNonNull(holder);byte[] payload=TrainingCheckpointCodec.encode(initial);UUID operation=initial.state().operationId();
        return transaction((c,tx)->{
            Entry old=row(c,operation),slot=current(c);
            if(old==null&&new TrainingHistory(access,connections).findOperation(c,operation)!=null)throw new FencedException();
            if(old!=null){
                if(old.status()==Status.ACTIVE&&holder.equals(old.holder())&&slot!=null&&slot.operation().equals(operation))return old;
                throw new FencedException();
            }
            if(slot!=null&&slot.status()==Status.ACTIVE)throw new FencedException();
            try(var p=c.prepareStatement("INSERT INTO training_checkpoint(pengguna_id,operation_id,status,holder_token,revision,format_version,payload,payload_hash,checkpoint_at) VALUES(?,?,'ACTIVE',?,1,?,?,?,?)")){
                p.setInt(1,access.owner());p.setString(2,operation.toString());p.setString(3,holder.toString());p.setInt(4,TrainingCheckpointCodec.VERSION);p.setBytes(5,payload);p.setString(6,TrainingCheckpointCodec.hash(payload));p.setObject(7,now());if(tx.execute(p)!=1)throw new SQLException();
            }
            try(var p=c.prepareStatement("INSERT INTO training_checkpoint_slot(pengguna_id,operation_id) VALUES(?,?) ON DUPLICATE KEY UPDATE operation_id=VALUES(operation_id)")){
                p.setInt(1,access.owner());p.setString(2,operation.toString());tx.execute(p);
            }
            return row(c,operation);
        });
    }
    public Entry claim(UUID operation,UUID expectedHolder,UUID newHolder)throws SQLException{
        Objects.requireNonNull(newHolder);
        return transaction((c,tx)->{
            Entry entry=current(c);if(entry==null||!entry.operation().equals(operation))throw new FencedException();
            entry=reconcile(c,tx,entry);if(entry.receipt()!=null)return entry;
            if(entry.status()!=Status.ACTIVE||entry.checkpoint()==null)throw new FencedException();
            if(newHolder.equals(entry.holder()))return entry; // Same takeover request after a lost acknowledgement.
            if(!Objects.equals(entry.holder(),expectedHolder))throw new FencedException();
            try(var p=c.prepareStatement("UPDATE training_checkpoint SET holder_token=?,revision=revision+1 WHERE pengguna_id=? AND operation_id=? AND status='ACTIVE'")){
                p.setString(1,newHolder.toString());p.setInt(2,access.owner());p.setString(3,operation.toString());if(tx.execute(p)!=1)throw new FencedException();
            }
            return row(c,operation);
        });
    }
    private Entry owned(Connection c,UUID operation,UUID holder)throws SQLException{
        Entry entry=current(c);
        if(entry==null||!entry.operation().equals(operation)||entry.status()!=Status.ACTIVE||!Objects.equals(entry.holder(),holder))throw new FencedException();
        return entry;
    }
    public Entry verify(UUID operation,UUID holder)throws SQLException{return transaction((c,tx)->owned(c,operation,holder));}
    public Entry write(UUID operation,UUID holder,long revision,TrainingCheckpoint checkpoint)throws SQLException{
        if(!operation.equals(checkpoint.state().operationId())||revision<1||revision==Long.MAX_VALUE)throw new IllegalArgumentException("Revisi checkpoint tidak valid.");
        byte[] payload=TrainingCheckpointCodec.encode(checkpoint);String hash=TrainingCheckpointCodec.hash(payload);
        return transaction((c,tx)->{
            Entry old=owned(c,operation,holder);
            if(revision<=old.revision()){
                if(revision==old.revision()&&old.checkpoint()!=null&&hash.equals(TrainingCheckpointCodec.hash(TrainingCheckpointCodec.encode(old.checkpoint()))))return old;
                throw new IllegalStateException("Checkpoint lama ditolak; revisi yang lebih baru sudah tersimpan.");
            }
            if(old.checkpoint()==null)throw new FencedException();
            validateProgress(old.checkpoint(),checkpoint);
            try(var p=c.prepareStatement("UPDATE training_checkpoint SET revision=?,format_version=?,payload=?,payload_hash=?,checkpoint_at=? WHERE pengguna_id=? AND operation_id=? AND holder_token=? AND status='ACTIVE' AND revision<?")){
                p.setLong(1,revision);p.setInt(2,TrainingCheckpointCodec.VERSION);p.setBytes(3,payload);p.setString(4,hash);p.setObject(5,now());p.setInt(6,access.owner());p.setString(7,operation.toString());p.setString(8,holder.toString());p.setLong(9,revision);
                if(tx.execute(p)!=1)throw new FencedException();
            }
            return row(c,operation);
        });
    }
    private static void validateProgress(TrainingCheckpoint previous,TrainingCheckpoint next){
        var a=previous.state();var b=next.state();
        if(a.phase()==TrainingSession.Phase.READY)return;
        if(b.phase()==TrainingSession.Phase.READY||!a.steps().equals(b.steps())||!Objects.equals(a.startedAt(),b.startedAt())
                ||!Objects.equals(previous.programId(),next.programId())||!Objects.equals(previous.programVersion(),next.programVersion())
                ||!previous.programName().equals(next.programName())||b.activeNanos()<a.activeNanos()||b.index()<a.index())throw new IllegalArgumentException("Progres latihan tidak boleh mundur.");
        for(int i=0;i<a.steps().size();i++)if(b.stepActive().get(i)<a.stepActive().get(i)
                ||a.outcomes().get(i)!=TrainingSession.Outcome.BELUM_SELESAI&&a.outcomes().get(i)!=b.outcomes().get(i))throw new IllegalArgumentException("Rincian progres tidak boleh mundur.");
        if(a.phase()==TrainingSession.Phase.SUMMARY&&(next.result()==null||!previous.result().fingerprint().equals(next.result().fingerprint())))throw new IllegalArgumentException("Ringkasan tersimpan tidak boleh berubah.");
    }
    public Entry discard(UUID operation,UUID expectedHolder)throws SQLException{
        return transaction((c,tx)->{
            Entry entry=current(c);if(entry==null||!entry.operation().equals(operation))throw new FencedException();
            entry=reconcile(c,tx,entry);if(entry.receipt()!=null)return entry;
            if(entry.status()==Status.DISCARDED)return entry;
            if(entry.status()!=Status.ACTIVE||!Objects.equals(entry.holder(),expectedHolder))throw new FencedException();
            try(var p=c.prepareStatement("UPDATE training_checkpoint SET status='DISCARDED',holder_token=NULL,payload=NULL,payload_hash=NULL,revision=revision+1 WHERE pengguna_id=? AND operation_id=? AND status='ACTIVE'")){
                p.setInt(1,access.owner());p.setString(2,operation.toString());if(tx.execute(p)!=1)throw new FencedException();
            }
            return row(c,operation);
        });
    }
    static void assertResultAllowed(Connection c,SessionAccess access,UUID operation,UUID holder)throws SQLException{
        try(var p=c.prepareStatement("SELECT status,holder_token FROM training_checkpoint WHERE pengguna_id=? AND operation_id=?")){
            p.setInt(1,access.owner());p.setString(2,operation.toString());try(var r=p.executeQuery()){
                if(r.next()&&(!r.getString(1).equals("ACTIVE")||holder==null||!holder.toString().equals(r.getString(2))))throw new FencedException();
            }
        }
        try(var p=c.prepareStatement("SELECT t.operation_id FROM training_checkpoint_slot s JOIN training_checkpoint t ON t.pengguna_id=s.pengguna_id AND t.operation_id=s.operation_id WHERE s.pengguna_id=? AND t.status='ACTIVE'")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){if(r.next()&&!r.getString(1).equals(operation.toString()))throw new FencedException();}
        }
    }
    static void complete(Connection c,WriteTransaction tx,SessionAccess access,UUID operation)throws SQLException{
        try(var p=c.prepareStatement("SELECT status FROM training_checkpoint WHERE pengguna_id=? AND operation_id=?")){
            p.setInt(1,access.owner());p.setString(2,operation.toString());try(var r=p.executeQuery()){if(r.next()&&r.getString(1).equals("COMPLETED"))return;}
        }
        try(var p=c.prepareStatement("INSERT INTO training_checkpoint(pengguna_id,operation_id,status,revision,format_version,checkpoint_at) VALUES(?,?,'COMPLETED',0,?,?) ON DUPLICATE KEY UPDATE status='COMPLETED',holder_token=NULL,payload=NULL,payload_hash=NULL,revision=revision+1")){
            p.setInt(1,access.owner());p.setString(2,operation.toString());p.setInt(3,TrainingCheckpointCodec.VERSION);p.setObject(4,now());tx.execute(p);
        }
    }
    private static LocalDateTime now(){return LocalDateTime.ofInstant(Instant.now(),ZoneOffset.UTC);}
}
