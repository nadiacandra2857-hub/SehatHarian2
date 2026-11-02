package com.sehatharian;

import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OwnershipServiceTest {
    private OwnershipFixtures f;
    private final LocalDate day=LocalDate.of(2026,10,31);
    @BeforeEach void setup() throws SQLException { f=new OwnershipFixtures(); seed(); }
    private void seed() {
        for(int owner=1;owner<=2;owner++) {
            int id=owner==1?11:22;
            f.db.add("aktivitas","id",id,"pengguna_id",owner,"tanggal",java.sql.Date.valueOf(day),"jenis_aktivitas","Synthetic","durasi_menit",owner==1?20:400,"keterangan","");
            f.db.add("hidrasi","id",id,"pengguna_id",owner,"tanggal",java.sql.Date.valueOf(day),"jumlah_ml",owner==1?250:4000,"catatan","");
            f.db.add("kebiasaan_makan","id",id,"pengguna_id",owner,"tanggal",java.sql.Date.valueOf(day),"waktu_makan","sarapan","menu","Synthetic","keterangan","");
            f.db.add("jadwal_tidur","id",id,"pengguna_id",owner,"tanggal",java.sql.Date.valueOf(day),"jam_tidur",Time.valueOf("23:00:00"),"jam_bangun",Time.valueOf("06:00:00"),"durasi_jam",owner==1?7.0:3.0,"memenuhi_standar",owner==1,"pengingat_aktif",true);
            f.db.add("riwayat_tantangan","id",id,"pengguna_id",owner,"tantangan_id",10,"tanggal",java.sql.Date.valueOf(day),"durasi_target_menit",5,"status","disarankan");
            f.db.add("evaluasi_mingguan","id",id,"pengguna_id",owner,"minggu_mulai",java.sql.Date.valueOf(day.minusDays(6)),"skor_konsistensi",10.0,"durasi_tidur_rata_rata",7.0,"memenuhi_standar_tidur",true,"ringkasan","Synthetic","rekomendasi","Synthetic");
        }
    }
    private List<Map<String,Object>> b(String table) { return f.db.snapshot(table).stream().filter(r->Integer.valueOf(2).equals(r.get("pengguna_id"))||table.equals("pengguna")&&Integer.valueOf(2).equals(r.get("id"))).toList(); }
    @ParameterizedTest @ValueSource(strings={"aktivitas","hidrasi","makan","tidur","tantangan","evaluasi","progres","profil","onboarding","password"})
    void anonymousDeniedBeforeStorage(String feature) throws Exception {
        f=new OwnershipFixtures(false);
        assertThrows(SecurityException.class,()-> { Object ignored = switch(feature) {
            case "aktivitas" -> f.kebiasaan().getAktivitasHariIni(1,day);
            case "hidrasi" -> f.kebiasaan().getTotalHidrasiMl(1,day);
            case "makan" -> f.kebiasaan().getMakanHariIni(1,day);
            case "tidur" -> f.tidur().getJadwalTerbaru(1);
            case "tantangan" -> f.tantangan().getTantanganByTanggal(1,day);
            case "evaluasi" -> f.evaluasi().buatEvaluasiMingguan(1,day);
            case "progres" -> f.progres().getProgresHari(1,day);
            case "profil" -> new PenggunaService(new PenggunaRepository(f.db::connection),f.session).updateProfil(1,"Synthetic",null,45,"dewasa","luar_rumah");
            case "onboarding" -> { new PenggunaService(new PenggunaRepository(f.db::connection),f.session).simpanOnboarding(1,1,"1 jam","INDOOR","DEWASA");yield null; }
            case "password" -> new PenggunaService(new PenggunaRepository(f.db::connection),f.session).updatePassword(1,"unused");
            default -> throw new AssertionError();
        }; });
        assertEquals(0,f.db.opens); assertTrue(f.db.commands.isEmpty());
    }
    @Test void listsRangesAndAggregatesContainOnlyA() throws Exception {
        assertEquals(1,f.kebiasaan().getAktivitasHariIni(1,day).size());
        assertEquals(250,f.kebiasaan().getTotalHidrasiMl(1,day));
        assertEquals(20,f.kebiasaan().getTotalDurasiAktivitas(1,day));
        assertEquals(1,f.kebiasaan().getMakanHariIni(1,day).getFirst().getPenggunaId());
        assertEquals(1,f.tidur().getJadwalTerbaru(1).getPenggunaId());
        assertEquals(1,f.tantangan().getTantanganByTanggal(1,day).getFirst().getPenggunaId());
        assertEquals(1,f.evaluasi().getDaftarEvaluasi(1).getFirst().getPenggunaId());
        var summary=f.evaluasi().getKonsistensi7HariTerakhir(1,day).getLast();
        assertEquals(250,summary.getTotalHidrasiMl()); assertEquals(20,summary.getDurasiAktivitasMenit());assertEquals(7.0,summary.getDurasiTidurJam());
        var progress=f.progres().getProgresHari(1,day);
        assertEquals(250,progress.getTotalHidrasiMl()); assertEquals(20,progress.getTotalAktivitasMenit());
    }
    @ParameterizedTest @ValueSource(strings={"aktivitas","hidrasi","makan","tidur","alarm","tantangan"})
    void foreignAndMissingRecordsAreIndistinguishableAndBReallyUnchanged(String kind) throws Exception {
        String table=switch(kind){case "makan"->"kebiasaan_makan";case "tidur","alarm"->"jadwal_tidur";case "tantangan"->"riwayat_tantangan";default->kind;};
        var before=b(table);
        assertFalse(mutate(kind,22)); assertFalse(mutate(kind,9999)); assertEquals(before,b(table));
        assertTrue(mutate(kind,11)); assertEquals(before,b(table));
    }
    private boolean mutate(String kind,int id) throws Exception {
        return switch(kind) {
            case "aktivitas"-> f.kebiasaan().hapusAktivitas(id);
            case "hidrasi"->f.kebiasaan().hapusHidrasi(id);
            case "makan"->f.kebiasaan().hapusMakan(id);
            case "tidur"->f.tidur().hapusCatatan(id);
            case "alarm"->f.tidur().updatePengingat(id,false);
            case "tantangan"->f.tantangan().selesaikanTantangan(id);
            default->throw new AssertionError();
        };
    }
    @Test void tamperedOwnerParametersAndProfileCannotChangeB() throws Exception {
        var before=b("pengguna");
        assertThrows(SecurityException.class,()->f.kebiasaan().tambahAktivitas(2,day,"Synthetic",20,""));
        assertThrows(SecurityException.class,()->f.kebiasaan().tambahHidrasi(2,day,250,""));
        assertThrows(SecurityException.class,()->f.kebiasaan().tambahMakan(2,day,"sarapan","Synthetic",""));
        assertThrows(SecurityException.class,()->f.tidur().catatJamTidur(2,day,LocalTime.of(23,0),LocalTime.of(6,0),true));
        assertThrows(SecurityException.class,()->f.tantangan().pastikanTantanganHariIni(f.accounts.findByEmail("b@example.test"),day));
        assertThrows(SecurityException.class,()->f.evaluasi().getDaftarEvaluasi(2));
        assertThrows(SecurityException.class,()->f.progres().getProgresHari(2,day));
        assertThrows(SecurityException.class,()->f.profiles().updateProfil(2,"Synthetic",null,30,"dewasa","luar_rumah"));
        assertThrows(SecurityException.class,()->f.profiles().simpanOnboarding(2,1,"1 jam","INDOOR","DEWASA"));
        assertThrows(SecurityException.class,()->f.profiles().updatePassword(2,"unused"));
        assertEquals(before,b("pengguna"));assertEquals(0,f.db.opens);
    }
    @Test void dtoOwnerAndRecordIdCannotOverrideRepositoryScope() throws Exception {
        var before=b("jadwal_tidur");
        var dto=new Aktivitas(0,2,day,"Synthetic",20,"");
        assertThrows(SecurityException.class,()->new AktivitasRepository(f.access,f.db::connection).simpan(dto));
        assertEquals(0,f.db.opens);
        var sleep=new JadwalTidur(22,1,day,LocalTime.of(22,0),LocalTime.of(6,0),8,true,false);
        assertThrows(SQLException.class,()->new JadwalTidurRepository(f.access,f.db::connection).simpan(sleep));
        assertEquals(before,b("jadwal_tidur"));
    }
    @Test void insertsAssignAAndNeverCreateRowsForB() throws Exception {
        var before=b("hidrasi");
        f.kebiasaan().tambahHidrasi(1,day,300,"");
        assertEquals(before,b("hidrasi"));assertEquals(550,f.kebiasaan().getTotalHidrasiMl(1,day));
        var insert=f.db.commands.stream().filter(c->c.sql().startsWith("insert")).findFirst().orElseThrow();
        assertEquals(1,insert.parameters().get(1));
    }
    @Test void weeklyEvaluationUpdatesAOnlyAndAllSqlMutationsCarryOwner() throws Exception {
        var before=b("evaluasi_mingguan");var sleepB=b("jadwal_tidur");
        var evaluation=f.evaluasi().buatEvaluasiMingguan(1,day);
        assertEquals(1,evaluation.getPenggunaId());assertEquals(7.0,evaluation.getDurasiTidurRataRata());
        assertEquals(before,b("evaluasi_mingguan"));
        f.tidur().catatJamTidur(1,day,LocalTime.of(22,0),LocalTime.of(6,0),false);
        assertEquals(sleepB,b("jadwal_tidur"));
        for(var command:f.db.commands) if(command.sql().startsWith("update")||command.sql().startsWith("delete")) {
            assertTrue(command.sql().contains("where id = ? and pengguna_id = ?"));
            assertEquals(1,command.parameters().get(Collections.max(command.parameters().keySet())));
        }
    }
    @Test void zeroAffectedRowsAndSqlFailureCannotBecomeSuccessOrFallback() throws Exception {
        var before=f.db.snapshot("hidrasi");
        f.db.zeroWrite=true;
        assertFalse(f.kebiasaan().hapusHidrasi(11));
        assertThrows(SQLException.class,()->f.kebiasaan().tambahHidrasi(1,day,300,""));
        assertFalse(f.profiles().updateProfil(1,"Synthetic",null,45,"dewasa","luar_rumah"));
        f.db.zeroWrite=false;f.db.failWrite=true;
        assertThrows(SQLException.class,()->f.kebiasaan().hapusHidrasi(11));
        assertEquals(before,f.db.snapshot("hidrasi"));
        f.db.failRead=true;
        assertThrows(SQLException.class,()->f.evaluasi().buatEvaluasiMingguan(1,day));
    }
    @Test void staleCommandCannotUseBSessionOrTouchAnyStorage() throws Exception {
        var old=f.kebiasaan();var before=b("hidrasi");f.loginB();
        assertThrows(SecurityException.class,()->old.tambahHidrasi(2,day,300,""));
        assertThrows(SecurityException.class,()->old.hapusHidrasi(22));
        assertEquals(0,f.db.opens);assertEquals(before,b("hidrasi"));
    }
    @Test void oldReadResultRejectedBeforeItCanReachUi() throws Exception {
        f.db.afterQuery=()->{try{f.loginB();}catch(SQLException e){throw new AssertionError(e);}};
        List<Aktivitas> ui=new ArrayList<>();
        assertThrows(SecurityException.class,()-> { var result=f.kebiasaan().getAktivitasHariIni(1,day);f.access.apply(()->ui.addAll(result)); });
        assertTrue(ui.isEmpty());
    }
    @Test void inFlightSqlMayFinishForAResultStillRejectedAndBNeverChanged() throws Exception {
        var before=b("riwayat_tantangan");
        f.db.afterWrite=()->{try{f.loginB();}catch(SQLException e){throw new AssertionError(e);}};
        assertThrows(SecurityException.class,()->f.tantangan().selesaikanTantangan(11));
        assertEquals(before,b("riwayat_tantangan"));
        assertEquals("selesai",f.db.table("riwayat_tantangan").getFirst().get("status")); // not a rollback claim
    }
    @Test void adapterReturningMixedOwnersIsRejectedRatherThanFedIntoEvaluation() throws Exception {
        var activities=new AktivitasRepository(f.access,f.db::connection) {
            @Override public List<Aktivitas> getByTanggal(int owner,LocalDate date) { return List.of(new Aktivitas(22,2,day,"Synthetic",400,"")); }
        };
        var service=new KebiasaanService(f.access,activities,new HidrasiRepository(f.access,f.db::connection),new KebiasaanMakanRepository(f.access,f.db::connection));
        assertThrows(SecurityException.class,()->service.getAktivitasHariIni(1,day));
    }
    @Test void noEvidenceDoesNotProduceHealthAdviceOrEvaluationWrite() throws Exception {
        f.db.table("aktivitas").clear();f.db.table("hidrasi").clear();f.db.table("jadwal_tidur").clear();f.db.table("riwayat_tantangan").clear();
        assertTrue(f.evaluasi().getKonsistensi7HariTerakhir(1,day).stream().noneMatch(RingkasanKonsistensi::hasRecordedData));
        var before=f.db.snapshot("evaluasi_mingguan");
        assertThrows(IllegalStateException.class,()->f.evaluasi().buatEvaluasiMingguan(1,day));
        assertEquals(before,f.db.snapshot("evaluasi_mingguan"));
    }
    @Test void missingHabitEvidenceIsDescribedAsUnknownNotPoorHealth() throws Exception {
        f.db.table("jadwal_tidur").clear();f.db.table("aktivitas").clear();
        var result=f.evaluasi().buatEvaluasiMingguan(1,day);
        assertTrue(result.getRekomendasi().contains("Belum ada catatan tidur"));
        assertTrue(result.getRekomendasi().contains("Belum ada catatan aktivitas"));
        assertFalse(result.getRekomendasi().contains("Durasi tidur Anda kurang"));
    }
    @Test void personalMemoryApisAreNotPublicAndCatalogRemainsGlobal() throws Exception {
        var allowed=Set.of("getInstance","getAllTantangan","getTantanganById","getTantanganByFilter","getAllRekomendasi","getFilteredRekomendasi");
        for(var method:InMemoryDataStore.class.getDeclaredMethods()) if(java.lang.reflect.Modifier.isPublic(method.getModifiers()))assertTrue(allowed.contains(method.getName()));
        assertFalse(f.tantangan().getAllKatalogTantangan().isEmpty());
        var anonymous=new OwnershipFixtures(false);assertFalse(anonymous.rekomendasi().getAllRekomendasi().isEmpty());
    }
    @Test void reminderSqlProjectionRejectsAnonymousAndForeignIdBeforeStorage() throws Exception {
        var provider=new SqlSleepScheduleProvider(f.session,f.db::connection);
        assertThrows(SecurityException.class,()->provider.latest(2));assertEquals(0,f.db.opens);
        assertEquals(LocalTime.of(23,0),provider.latest(1).bedtime());
        f.auth.logout();int opens=f.db.opens;assertThrows(SecurityException.class,()->provider.latest(1));assertEquals(opens,f.db.opens);
    }
    @Test void profileScopeCannotBeBorrowedFromAnotherContextWithSameUserId() throws Exception {
        var other=new OwnershipFixtures();
        assertThrows(SecurityException.class,()->new PenggunaService(new PenggunaRepository(f.db::connection),f.session).forView(other.access));
    }
    @Test void serviceRejectsForeignInsertEvenWhenStorageAdapterHasNoOwnerChecks() throws Exception {
        int[] called={0};
        var hydration=new HidrasiRepository(f.access,f.db::connection) {
            @Override public int simpan(Hidrasi input) { called[0]++; return 123; }
        };
        var service=new KebiasaanService(f.access,new AktivitasRepository(f.access,f.db::connection),hydration,new KebiasaanMakanRepository(f.access,f.db::connection));
        assertThrows(SecurityException.class,()->service.tambahHidrasi(2,day,300,""));
        assertEquals(0,called[0]);assertEquals(0,f.db.opens);
    }
    @Test void mutableDtoDuringConnectionOpeningCannotBindAnotherOwner() throws Exception {
        var before=b("hidrasi");var input=new Hidrasi(0,1,day,300,"");
        var repo=new HidrasiRepository(f.access,()->{input.setPenggunaId(2);return f.db.connection();});
        assertTrue(repo.simpan(input)>0);assertEquals(before,b("hidrasi"));
        assertEquals(1,f.db.commands.getLast().parameters().get(1));
    }
    @Test void staleConnectionIsClosedBeforeAnyStatementIsIssued() throws Exception {
        var repo=new HidrasiRepository(f.access,()->{f.loginB();return f.db.connection();});
        assertThrows(SecurityException.class,()->repo.getByTanggal(1,day));
        assertEquals(1,f.db.closedConnections);assertTrue(f.db.commands.isEmpty());
    }
    @Test void profileOnboardingAndPasswordWritesOnlyBindVerifiedAccountPrimaryKey() throws Exception {
        var before=b("pengguna");
        assertTrue(f.profiles().updateProfil(1,"Updated Synthetic",null,60,"dewasa","luar_rumah"));
        f.profiles().simpanOnboarding(1,2,"2 jam","INDOOR","DEWASA");
        assertTrue(f.profiles().updatePassword(1,f.accounts.findByEmail("a@example.test").getPasswordHash()));
        assertEquals(before,b("pengguna"));
        for(var c:f.db.commands) if(c.sql().startsWith("update pengguna")) {
            assertTrue(c.sql().endsWith("where id = ?"));
            assertEquals(1,c.parameters().get(Collections.max(c.parameters().keySet())));
        }
        var p=f.accounts.findByEmail("b@example.test");
        assertThrows(SecurityException.class,()->new PenggunaRepository(f.db::connection).updatePengguna(p,f.access));
        assertEquals(before,b("pengguna"));
    }
    @Test void historicalEvaluationUsesRequestedRangeDespiteManyNewerSleepRecords() throws Exception {
        for(int i=1;i<=25;i++)f.db.add("jadwal_tidur","id",200+i,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(day.plusDays(i)),"jam_tidur",Time.valueOf("23:00:00"),"jam_bangun",Time.valueOf("06:00:00"),"durasi_jam",2.0,"memenuhi_standar",false,"pengingat_aktif",true);
        var result=f.evaluasi().getKonsistensi7HariTerakhir(1,day).getLast();
        assertEquals(7.0,result.getDurasiTidurJam());
        assertTrue(f.db.commands.stream().anyMatch(c->c.sql().contains("from jadwal_tidur where pengguna_id = ? and tanggal between ? and ?")));
    }
    @Test void profileReadMissingOrUnavailableDoesNotRecoverDefaultUser() throws Exception {
        var repo=new PenggunaRepository(f.db::connection);
        assertThrows(SecurityException.class,()->repo.getById(2,f.access));
        f.db.table("pengguna").removeIf(r->Integer.valueOf(1).equals(r.get("id")));
        assertNull(repo.getById(1,f.access));f.db.failRead=true;
        assertThrows(SQLException.class,()->repo.getById(1,f.access));
    }
}
