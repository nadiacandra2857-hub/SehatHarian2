package com.sehatharian.training;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.service.KebiasaanService;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Owned durable results. All writers and operation-status probes serialize on the same owner row. */
public final class TrainingHistory extends OwnedRepository {
    public record Saved(long id,int activityId,TrainingSnapshot snapshot) {}
    public TrainingHistory(SessionAccess access){super(access);}
    public TrainingHistory(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    private void lockOwner(Connection c)throws SQLException{
        try(var p=c.prepareStatement("SELECT id FROM pengguna WHERE id=? FOR UPDATE")){
            p.setInt(1,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new SQLException("Pemilik tidak tersedia.");}
        }access.owner();
    }
    public Saved save(TrainingSnapshot snapshot)throws SQLException {return save(snapshot,null);}
    public Saved save(TrainingSnapshot snapshot,UUID holder)throws SQLException {
        access.owner();
        if(snapshot.minutes()<1||snapshot.minutes()>720)throw new IllegalArgumentException("Durasi aktivitas harus 1–720 menit.");
        var tx=new WriteTransaction(access,connections);
        try{
            var c=tx.open();lockOwner(c);
            Saved existing=findOperation(c,snapshot.operationId());
            if(existing!=null){
                if(!existing.snapshot().fingerprint().equals(snapshot.fingerprint()))throw new IllegalArgumentException("Identitas operasi sudah dipakai untuk hasil lain.");
                TrainingCheckpoints.complete(c,tx,access,snapshot.operationId());
                return tx.confirm(existing);
            }
            TrainingCheckpoints.assertResultAllowed(c,access,snapshot.operationId(),holder);
            var habits=new KebiasaanService(access);
            int activity=habits.tambahLatihanDalamTransaksi(c,tx,snapshot);
            long id;
            try(var p=c.prepareStatement("INSERT INTO training_session(pengguna_id,operation_id,payload_hash,activity_id,started_at,ended_at,active_nanos,stopped,program_id,program_version,program_name) VALUES(?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,access.owner());p.setString(2,snapshot.operationId().toString());p.setString(3,snapshot.fingerprint());p.setInt(4,activity);
                p.setObject(5,LocalDateTime.ofInstant(snapshot.startedAt(),ZoneOffset.UTC));p.setObject(6,LocalDateTime.ofInstant(snapshot.endedAt(),ZoneOffset.UTC));
                p.setLong(7,snapshot.activeNanos());p.setBoolean(8,snapshot.stopped());p.setObject(9,snapshot.programId());p.setObject(10,snapshot.programVersion());p.setString(11,snapshot.programName());
                if(tx.execute(p)!=1)throw new SQLException("Sesi belum tersimpan.");try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("ID sesi belum tersedia.");id=r.getLong(1);}
            }
            int position=0;
            for(var step:snapshot.steps())try(var p=c.prepareStatement("INSERT INTO training_step(session_id,position,movement_ref,movement_name,media_ref,source_ref,category,repetitions,target,active_nanos,outcome,repetition_confirmed,confirmed_repetitions) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                p.setLong(1,id);p.setInt(2,position++);p.setString(3,step.movementRef());p.setString(4,step.name());p.setString(5,step.mediaRef());p.setString(6,step.sourceRef());p.setString(7,step.category());p.setBoolean(8,step.repetitions());p.setInt(9,step.target());p.setLong(10,step.activeNanos());p.setString(11,step.outcome().name());p.setObject(12,step.repetitionConfirmed());p.setObject(13,step.confirmedRepetitions());
                if(tx.execute(p)!=1)throw new SQLException("Langkah belum tersimpan.");
            }
            TrainingCheckpoints.complete(c,tx,access,snapshot.operationId());
            return tx.confirm(new Saved(id,activity,snapshot));
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    /** A successful empty probe has waited for any same-owner writer to finish/rollback.
     * A later retry STILL takes the lock and checks the unique operation before inserting. */
    public Optional<Saved> status(UUID operation)throws SQLException {
        var tx=new WriteTransaction(access,connections);
        try{var c=tx.open();lockOwner(c);return tx.confirm(Optional.ofNullable(findOperation(c,operation)));}
        catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    Saved findOperation(Connection c,UUID operation)throws SQLException{
        try(var p=c.prepareStatement("SELECT * FROM training_session WHERE pengguna_id=? AND operation_id=?")){
            p.setInt(1,access.owner());p.setString(2,operation.toString());try(var r=p.executeQuery()){return r.next()?map(c,r):null;}
        }
    }
    public Saved byActivity(int activity)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT * FROM training_session WHERE pengguna_id=? AND activity_id=?")){
            p.setInt(1,access.owner());p.setInt(2,activity);try(var r=p.executeQuery()){return access.result(r.next()?map(c,r):null);}
        }
    }
    public Saved detail(long id)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT * FROM training_session WHERE pengguna_id=? AND id=?")){
            p.setInt(1,access.owner());p.setLong(2,id);try(var r=p.executeQuery()){return access.result(r.next()?map(c,r):null);}
        }
    }
    public List<Saved> list(int days,int offset)throws SQLException {
        if(!Set.of(7,30,90,0).contains(days)||offset<0)throw new IllegalArgumentException("Periode tidak valid.");
        Instant start=days==0?Instant.parse("1970-01-01T00:00:00Z"):LocalDate.now(ZoneId.of("Asia/Jakarta")).minusDays(days-1).atStartOfDay(ZoneId.of("Asia/Jakarta")).toInstant();
        try(var c=open();var p=c.prepareStatement("SELECT * FROM training_session WHERE pengguna_id=? AND ended_at>=? ORDER BY ended_at DESC,id DESC LIMIT 20 OFFSET ?")){
            p.setInt(1,access.owner());p.setObject(2,LocalDateTime.ofInstant(start,ZoneOffset.UTC));p.setInt(3,offset);
            List<Saved> rows=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())rows.add(map(c,r));}return access.result(List.copyOf(rows));
        }
    }
    private Saved map(Connection c,ResultSet r)throws SQLException {
        access.owner(r.getInt("pengguna_id"));long id=r.getLong("id");List<TrainingSnapshot.Step> steps=new ArrayList<>();
        try(var p=c.prepareStatement("SELECT s.* FROM training_step s JOIN training_session t ON t.id=s.session_id WHERE s.session_id=? AND t.pengguna_id=? ORDER BY s.position")){
            p.setLong(1,id);p.setInt(2,access.owner());try(var s=p.executeQuery()){while(s.next()){
                Object confirmed=s.getObject("repetition_confirmed");Object count=s.getObject("confirmed_repetitions");
                steps.add(new TrainingSnapshot.Step(s.getString("movement_ref"),s.getString("movement_name"),s.getString("media_ref"),s.getString("source_ref"),s.getString("category"),s.getBoolean("repetitions"),s.getInt("target"),s.getLong("active_nanos"),TrainingSession.Outcome.valueOf(s.getString("outcome")),confirmed==null?null:s.getBoolean("repetition_confirmed"),count==null?null:s.getInt("confirmed_repetitions")));
            }}
        }
        var snapshot=new TrainingSnapshot(UUID.fromString(r.getString("operation_id")),r.getObject("started_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),r.getObject("ended_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),r.getLong("active_nanos"),r.getBoolean("stopped"),(Integer)r.getObject("program_id"),(Integer)r.getObject("program_version"),r.getString("program_name"),steps);
        if(!snapshot.fingerprint().equals(r.getString("payload_hash")))throw new SQLException("Integritas riwayat belum dapat dikonfirmasi.");
        return access.result(new Saved(id,r.getInt("activity_id"),snapshot));
    }
}
