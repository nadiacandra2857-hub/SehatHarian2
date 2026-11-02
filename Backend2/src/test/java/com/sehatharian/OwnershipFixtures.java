package com.sehatharian;

import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import org.mindrot.jbcrypt.BCrypt;
import java.time.*;
import java.sql.*;
import java.util.*;

/** Isolated fixture used by legacy service regressions and ownership tests. */
public class OwnershipFixtures {
    public static final String SECRET="Synthetic owner fixture secret";
    private static final String HASH=BCrypt.hashpw(SECRET,BCrypt.gensalt(4));
    public final OwnedJdbc db=new OwnedJdbc();
    public final UserSession session=new UserSession();
    public final AccountRepository accounts=new AccountRepository() {
        public Pengguna findByEmail(String email) {
            int id=email.equals("a@example.test")?1:email.equals("b@example.test")?2:0;
            return id==0?null:new Pengguna(id,"Synthetic User "+id,email,HASH,null,"dewasa","luar_rumah",45,true);
        }
        public boolean existsByEmail(String email) { return findByEmail(email)!=null; }
        public int createPengguna(Pengguna p) { throw new AssertionError("No registration writes in ownership fixture"); }
    };
    public final AuthService auth=new AuthService(accounts,session);
    public SessionAccess access;
    public OwnershipFixtures() throws SQLException { this(true); }
    public OwnershipFixtures(boolean login) throws SQLException {
        if(login) auth.login("a@example.test",SECRET);
        access=new SessionAccess(session);
        for(int id=1;id<=2;id++) db.add("pengguna","id",id,"nama","Synthetic User "+id,"email",id==1?"a@example.test":"b@example.test","password_hash",HASH,"golongan_usia","dewasa","golongan_kesibukan","luar_rumah","waktu_luang_menit",45,"onboarding_selesai",true);
        db.add("tantangan","id",10,"judul","Synthetic challenge","deskripsi","Fixture","kategori","hidrasi");
    }
    public void loginB() throws SQLException { auth.login("b@example.test",SECRET); }
    public KebiasaanService kebiasaan() { return new KebiasaanService(access,new AktivitasRepository(access,db::connection),new HidrasiRepository(access,db::connection),new KebiasaanMakanRepository(access,db::connection)); }
    public TidurService tidur() { return new TidurService(access,new JadwalTidurRepository(access,db::connection)); }
    public EvaluasiService evaluasi() { return new EvaluasiService(access,new RiwayatTantanganRepository(access,db::connection),new JadwalTidurRepository(access,db::connection),new AktivitasRepository(access,db::connection),new HidrasiRepository(access,db::connection),new EvaluasiMingguanRepository(access,db::connection)); }
    public ProgresHarianService progres() { return new ProgresHarianService(access,new ProgresHarianRepository(access,db::connection)); }
    public PenggunaService profiles() { return new PenggunaService(new PenggunaRepository(db::connection),session).forView(access); }
    public TantanganService tantangan() { return new TantanganService(access,catalog(),new RiwayatTantanganRepository(access,db::connection)); }
    public TantanganRepository catalog() {
        return new TantanganRepository() {
            public List<Tantangan> getAll() { return List.of(new Tantangan(10,"Synthetic challenge","Fixture","hidrasi",5,"semua","semua")); }
            public List<Tantangan> getByFilter(String context,String age,int duration) { return getAll(); }
            public Tantangan getById(int id) { return getAll().stream().filter(t->t.getId()==id).findFirst().orElse(null); }
        };
    }
    public RekomendasiService rekomendasi() {
        return new RekomendasiService(new RekomendasiRepository() {
            public List<Rekomendasi> getAll() { return List.of(
                new Rekomendasi(1,"aktivitas","Synthetic activity","Fixture","semua","semua",5),
                new Rekomendasi(2,"menu","Synthetic menu","Fixture","semua","semua",10),
                new Rekomendasi(3,"workout","Synthetic workout","Fixture","semua","semua",10)); }
            public List<Rekomendasi> getFiltered(String type,String context,String age,int minutes) {
                return getAll().stream().filter(r->type==null||type.isEmpty()||r.getTipe().equals(type)).filter(r->minutes<=0||r.getDurasiMenit()<=minutes).toList();
            }
        });
    }
    public void seedEvidence() throws SQLException { kebiasaan().tambahHidrasi(access.owner(),LocalDate.now(),300,"Synthetic"); }
}
