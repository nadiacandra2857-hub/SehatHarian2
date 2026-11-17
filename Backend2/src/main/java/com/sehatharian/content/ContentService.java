package com.sehatharian.content;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import com.sehatharian.catalog.TeamExercises;
import java.sql.*;
import java.util.*;

/** Versioned content only. No API grants access to personal health records. */
public final class ContentService extends OwnedRepository {
    public enum Kind { MOVEMENT, PROGRAM, MENU, CHALLENGE, EDUCATION }
    public enum State { DRAFT, REVIEWED, PUBLISHED, SUPERSEDED, ARCHIVED }
    public record Content(int id,String key,Kind kind,String title,String body,State state,int revision) { @Override public String toString(){return title+" · v"+revision+" · "+state;} }
    public record Step(int movementId,int target,boolean repetitions) {}
    public record Program(Content content,List<Step> steps) {public Program{steps=List.copyOf(steps);}}
    public ContentService(SessionAccess access){super(access);}
    public ContentService(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    public List<Content> published(Kind kind)throws SQLException{return list(kind,false,"");}
    public List<Content> adminList(Kind kind,String query)throws SQLException{access.requireAdmin();return list(kind,true,query);}
    private List<Content> list(Kind kind,boolean drafts,String query)throws SQLException {
        try(var c=open();var p=c.prepareStatement("SELECT * FROM content_version WHERE kind=? AND title LIKE ?"+(drafts?"":" AND state='PUBLISHED'")+" ORDER BY id DESC")){
            p.setString(1,kind.name());p.setString(2,"%"+query+"%");List<Content> out=new ArrayList<>();try(var r=p.executeQuery()){while(r.next())out.add(map(r));}return access.result(out);
        }
    }
    public Program preview(int id)throws SQLException{access.requireAdmin();try(var c=open()){return program(c,id,false);}}
    public Program publishedProgram(int id)throws SQLException{try(var c=open()){return access.result(program(c,id,true));}}
    private Program program(Connection c,int id,boolean publicOnly)throws SQLException {
        Content content=get(c,id);if(content.kind()!=Kind.PROGRAM||publicOnly&&content.state()!=State.PUBLISHED)throw new SecurityException("Program belum diterbitkan.");
        List<Step> steps=new ArrayList<>();try(var p=c.prepareStatement("SELECT movement_id,target,repetitions FROM content_step WHERE program_id=? ORDER BY position")){p.setInt(1,id);try(var r=p.executeQuery()){while(r.next())steps.add(new Step(r.getInt(1),r.getInt(2),r.getBoolean(3)));}}
        return new Program(content,steps);
    }
    public Content movementForPublishedStep(int programId,int movementId)throws SQLException {
        try(var c=open()){var program=program(c,programId,true);if(program.steps().stream().noneMatch(s->s.movementId()==movementId))throw new SecurityException();Content movement=get(c,movementId);if(!Set.of(State.PUBLISHED,State.SUPERSEDED).contains(movement.state()))throw new SecurityException();return access.result(movement);}
    }
    private static Content map(ResultSet r)throws SQLException{return new Content(r.getInt("id"),r.getString("content_key"),Kind.valueOf(r.getString("kind")),r.getString("title"),r.getString("body"),State.valueOf(r.getString("state")),r.getInt("revision"));}
    private Content get(Connection c,int id)throws SQLException{try(var p=c.prepareStatement("SELECT * FROM content_version WHERE id=?")){p.setInt(1,id);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Konten tidak ditemukan.");return map(r);}}}
    public static void validate(Kind kind,String title,String body,List<Step> steps) {
        if(kind==null||title==null||title.isBlank()||title.length()>150||body==null||body.isBlank()||body.length()>16000)throw new IllegalArgumentException("Judul dan isi wajib, maksimal 150 dan 16.000 karakter.");
        if(kind==Kind.MOVEMENT&&TeamExercises.find(body.strip()).isEmpty())throw new IllegalArgumentException("Pilih ID video tim yang tersedia.");
        if(kind==Kind.CHALLENGE){String[] parts=body.strip().split("\\|",-1);if(parts.length!=2||!com.sehatharian.service.HabitInsights.UNITS.containsKey(parts[0]))throw new IllegalArgumentException("Kriteria: category|target, misalnya hydration|1500.");double target=Double.parseDouble(parts[1]);if(!Double.isFinite(target)||target<=0||target>10000)throw new IllegalArgumentException("Target tidak valid.");}
        if(kind==Kind.PROGRAM&&(steps.isEmpty()||steps.size()>20))throw new IllegalArgumentException("Program memerlukan 1–20 langkah.");
        for(Step s:steps)if(s.target()<1||s.target()>3600||s.movementId()<1)throw new IllegalArgumentException("Langkah tidak valid.");
    }
    public int saveDraft(Integer baseId,Kind kind,String title,String body,List<Step> steps)throws SQLException {
        return saveDraft(baseId,kind,title,body,steps,null);
    }
    public int saveMenuDraft(Integer baseId,String title,String body,com.sehatharian.nutrition.MenuModels.Composition composition)throws SQLException {
        return saveDraft(baseId,Kind.MENU,title,body,List.of(),java.util.Objects.requireNonNull(composition));
    }
    /** Stable external identity is isolated from admin-owned content. Imports always create DRAFT. */
    public int importMenu(String itemKey,String sourceVersion,String payloadHash,String title,String body,com.sehatharian.nutrition.MenuModels.Composition composition,String image,String caption)throws SQLException {
        access.requireAdmin();
        if(itemKey==null||!itemKey.matches("curated:[a-z0-9._:-]{1,140}")||sourceVersion==null||!sourceVersion.matches("[a-zA-Z0-9._:-]{1,80}")||payloadHash==null||!payloadHash.matches("[a-f0-9]{64}")||image==null||!image.matches("/images/(menus/[a-z0-9-]+|home/nutrition)\\.jpg")||caption==null||caption.isBlank()||caption.length()>255)throw new IllegalArgumentException("Identitas impor atau ilustrasi tidak valid.");
        return saveDraft(null,Kind.MENU,title,body,List.of(),Objects.requireNonNull(composition),new ImportData(itemKey,sourceVersion,payloadHash,image,caption));
    }
    private record ImportData(String key,String version,String hash,String image,String caption){}
    private int saveDraft(Integer baseId,Kind kind,String title,String body,List<Step> steps,com.sehatharian.nutrition.MenuModels.Composition composition)throws SQLException {
        return saveDraft(baseId,kind,title,body,steps,composition,null);
    }
    private int saveDraft(Integer baseId,Kind kind,String title,String body,List<Step> steps,com.sehatharian.nutrition.MenuModels.Composition composition,ImportData imported)throws SQLException {
        access.requireAdmin();validate(kind,title,body,steps);WriteTransaction tx=new WriteTransaction(access,connections);
        try{var c=tx.open();lock(c);String key=UUID.randomUUID().toString();int revision=1;
            if(imported!=null){
                try(var p=c.prepareStatement("SELECT menu_id,payload_hash FROM curated_menu_import WHERE item_key=? AND source_version=?")){p.setString(1,imported.key());p.setString(2,imported.version());try(var r=p.executeQuery()){if(r.next()){if(!r.getString(2).equals(imported.hash()))throw new IllegalArgumentException("Versi sumber yang sama berisi perubahan; naikkan versi paket.");return tx.confirm(r.getInt(1));}}}
                key=UUID.nameUUIDFromBytes(("SehatHarian:"+imported.key()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                Integer latest=null;try(var p=c.prepareStatement("SELECT menu_id FROM curated_menu_import WHERE item_key=? ORDER BY import_id DESC LIMIT 1")){p.setString(1,imported.key());try(var r=p.executeQuery()){if(r.next())latest=r.getInt(1);}}
                try(var p=c.prepareStatement("SELECT id,revision FROM content_version WHERE content_key=? ORDER BY revision DESC LIMIT 1")){p.setString(1,key);try(var r=p.executeQuery()){if(r.next()){if(latest==null||r.getInt(1)!=latest)throw new IllegalArgumentException("Konten sudah direvisi admin; impor tidak menimpa revisi tersebut.");revision=r.getInt(2)+1;}}}
            }
            if(baseId!=null){var base=get(c,baseId);if(base.kind()!=kind)throw new IllegalArgumentException("Jenis revisi tidak boleh berubah.");key=base.key();try(var p=c.prepareStatement("SELECT MAX(revision) FROM content_version WHERE content_key=?")){p.setString(1,key);try(var r=p.executeQuery()){r.next();revision=r.getInt(1)+1;}}}
            int id;try(var p=c.prepareStatement("INSERT INTO content_version(content_key,kind,title,body,state,revision,created_by) VALUES(?,?,?,?,'DRAFT',?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setString(1,key);p.setString(2,kind.name());p.setString(3,title.strip());p.setString(4,body.strip());p.setInt(5,revision);p.setInt(6,access.requireAdmin());tx.execute(p);try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException();id=r.getInt(1);}}
            if(kind==Kind.PROGRAM)for(int i=0;i<steps.size();i++){var step=steps.get(i);Content movement=get(c,step.movementId());if(movement.kind()!=Kind.MOVEMENT)throw new IllegalArgumentException("Referensi bukan gerakan.");try(var p=c.prepareStatement("INSERT INTO content_step(program_id,position,movement_id,target,repetitions) VALUES(?,?,?,?,?)")){p.setInt(1,id);p.setInt(2,i);p.setInt(3,step.movementId());p.setInt(4,step.target());p.setBoolean(5,step.repetitions());tx.execute(p);}}
            if(kind==Kind.MENU){var mapping=composition!=null?composition:baseId==null?com.sehatharian.nutrition.MenuModels.Composition.unknown():com.sehatharian.nutrition.MenuComposition.load(c,baseId).composition();com.sehatharian.nutrition.MenuComposition.save(c,tx,access,id,mapping);
                if(imported!=null){
                    try(var p=c.prepareStatement("INSERT INTO menu_presentation(menu_id,image_path,caption) VALUES(?,?,?)")){p.setInt(1,id);p.setString(2,imported.image());p.setString(3,imported.caption());tx.execute(p);}
                    try(var p=c.prepareStatement("INSERT INTO curated_menu_import(item_key,source_version,payload_hash,menu_id) VALUES(?,?,?,?)")){p.setString(1,imported.key());p.setString(2,imported.version());p.setString(3,imported.hash());p.setInt(4,id);tx.execute(p);}
                }else if(baseId!=null)try(var p=c.prepareStatement("INSERT INTO menu_presentation(menu_id,image_path,caption) SELECT ?,image_path,caption FROM menu_presentation WHERE menu_id=?")){p.setInt(1,id);p.setInt(2,baseId);tx.execute(p);}
            }
            return tx.confirm(id);
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    // Serialize CMS graph edits by locking a real durable row, including an initially empty catalogue.
    private void lock(Connection c)throws SQLException {try(var p=c.prepareStatement("SELECT id FROM content_guard WHERE id=1 FOR UPDATE");var r=p.executeQuery()){while(r.next()){} }access.requireAdmin();}
    public static boolean transitionAllowed(State from,State to){return from==State.DRAFT&&to==State.REVIEWED||from==State.REVIEWED&&to==State.PUBLISHED||Set.of(State.DRAFT,State.REVIEWED,State.PUBLISHED,State.SUPERSEDED).contains(from)&&to==State.ARCHIVED;}
    public boolean transition(int id,State next)throws SQLException {
        access.requireAdmin();WriteTransaction tx=new WriteTransaction(access,connections);
        try{var c=tx.open();lock(c);Content item=get(c,id);if(!transitionAllowed(item.state(),next))throw new IllegalArgumentException("Transisi status tidak diizinkan.");
            if(item.kind()==Kind.MENU&&next==State.REVIEWED)com.sehatharian.nutrition.MenuComposition.review(c,tx,access,id);
            if(next==State.PUBLISHED){
                if(item.kind()==Kind.MENU){var mapped=com.sehatharian.nutrition.MenuComposition.load(c,id);if(mapped.composition().completeness()==com.sehatharian.nutrition.MenuModels.Completeness.COMPLETE&&!mapped.eligible())throw new IllegalArgumentException("Pemetaan berubah atau belum ditinjau. Buat dan tinjau draft revisi.");}List<Step> steps=item.kind()==Kind.PROGRAM?program(c,id,false).steps():List.of();validate(item.kind(),item.title(),item.body(),steps);
                for(Step step:steps){Content movement=get(c,step.movementId());if(movement.kind()!=Kind.MOVEMENT||!Set.of(State.PUBLISHED,State.SUPERSEDED).contains(movement.state())||TeamExercises.find(movement.body()).isEmpty())throw new IllegalArgumentException("Semua gerakan program harus sudah terbit dan memiliki video tim valid.");}
                try(var p=c.prepareStatement("UPDATE content_version SET state='SUPERSEDED' WHERE content_key=? AND state='PUBLISHED'")){p.setString(1,item.key());tx.execute(p);}}
            if(next==State.ARCHIVED&&item.kind()==Kind.MOVEMENT)try(var p=c.prepareStatement("SELECT COUNT(*) FROM content_step s JOIN content_version p ON p.id=s.program_id WHERE s.movement_id=? AND p.state='PUBLISHED'")){p.setInt(1,id);try(var r=p.executeQuery()){r.next();if(r.getInt(1)>0)throw new IllegalStateException("Gerakan masih dipakai program terbit. Arsipkan atau revisi program dahulu.");}}
            try(var p=c.prepareStatement("UPDATE content_version SET state=? WHERE id=? AND state=?")){p.setString(1,next.name());p.setInt(2,id);p.setString(3,item.state().name());access.requireAdmin();return tx.affected(tx.execute(p));}
        }catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
}
