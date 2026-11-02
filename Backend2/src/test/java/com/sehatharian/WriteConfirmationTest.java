package com.sehatharian;

import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.sehatharian.storage.WriteResult.Status.*;

class WriteConfirmationTest {
    private OwnershipFixtures f;
    private final LocalDate day=LocalDate.of(2026,10,31);
    @BeforeEach void setup() throws SQLException { f=new OwnershipFixtures(); }
    private WriteResult<Void> addWater() {
        return WriteResult.attempt(()->{ f.kebiasaan().tambahHidrasi(1,day,250,"Synthetic");return null; });
    }
    @Test void profileCommitPrecedesSnapshotUpdateAndDurableStateChange() throws Exception {
        var before=f.db.snapshot("pengguna");
        f.db.beforeCommit=()-> {
            assertEquals("Synthetic User 1",f.session.getUserName());
            assertEquals(before,f.db.snapshot("pengguna"));
        };
        assertTrue(f.profiles().updateProfil(1,"Confirmed Name",null,60,"dewasa","luar_rumah"));
        assertEquals(1,f.db.commits);assertEquals("Confirmed Name",f.session.getUserName());
        assertEquals("Confirmed Name",f.db.table("pengguna").getFirst().get("nama"));
    }
    @Test void failedConnectionBeforeExecutionIsKnownFailure() throws Exception {
        var repo=new HidrasiRepository(f.access,()->{throw new SQLException("Synthetic connect failure");});
        var result=WriteResult.attempt(()->repo.simpan(new Hidrasi(0,1,day,250,"")));
        assertEquals(GAGAL,result.status());assertEquals(0,f.db.executions);assertEquals(0,f.db.commits);
    }
    @Test void executeFailureWithAcknowledgedRollbackLeavesNoStoredChange() {
        f.db.failAfterExecute=true;var before=f.db.snapshot("hidrasi");
        var result=addWater();assertEquals(GAGAL,result.status());
        assertEquals(before,f.db.snapshot("hidrasi"));assertEquals(1,f.db.rollbacks);assertEquals(0,f.db.commits);
    }
    @Test void rollbackAcknowledgementFailureIsNotCertainFailure() {
        f.db.failAfterExecute=true;f.db.failRollback=true;
        assertEquals(BELUM_PASTI,addWater().status());assertEquals(1,f.db.executions);
    }
    @Test void rollbackWarningDoesNotProveAllChangesWereUndone() {
        f.db.failAfterExecute=true;f.db.rollbackWarning=true;
        assertEquals(BELUM_PASTI,addWater().status());assertEquals(0,f.db.commits);
    }
    @Test void missingCommitAcknowledgementCanMeanRowAlreadyStored() {
        f.db.loseCommitAck=true;
        assertEquals(BELUM_PASTI,addWater().status());
        assertEquals(1,f.db.table("hidrasi").size());assertEquals(1,f.db.executions);assertEquals(1,f.db.commits);
    }
    @Test void commitFailureCannotBeDeclaredFailedEvenIfSubsequentRollbackAcknowledged() {
        f.db.failCommitBeforeApply=true;
        assertEquals(BELUM_PASTI,addWater().status());assertTrue(f.db.table("hidrasi").isEmpty());
        assertTrue(f.db.rollbacks>0);
    }
    @ParameterizedTest @ValueSource(strings={"statement","connection","restore"})
    void cleanupAfterConfirmedCommitDoesNotEraseSuccess(String fault) {
        f.db.afterCommit=()-> {
            f.db.failStatementClose=fault.equals("statement");f.db.failConnectionClose=fault.equals("connection");f.db.failRestore=fault.equals("restore");
        };
        assertEquals(BERHASIL,addWater().status());assertEquals(1,f.db.table("hidrasi").size());assertEquals(1,f.db.commits);assertEquals(0,f.db.rollbacks);
    }
    @ParameterizedTest @ValueSource(strings={"missing","invalid","read"})
    void generatedKeyMustBeAvailableBeforeCommit(String fault) {
        f.db.missingKey=fault.equals("missing");f.db.invalidKey=fault.equals("invalid");f.db.failKey=fault.equals("read");
        var input=new Hidrasi(0,1,day,250,"");
        var result=WriteResult.attempt(()->new HidrasiRepository(f.access,f.db::connection).simpan(input));
        assertEquals(GAGAL,result.status());assertEquals(0,f.db.commits);assertTrue(f.db.table("hidrasi").isEmpty());assertEquals(0,input.getId());
    }
    @Test void zeroAffectedRowsDoNotCommitOrUpdateSnapshot() throws Exception {
        f.db.zeroWrite=true;
        var result=WriteResult.attempt(()->f.profiles().updateProfil(1,"Changed Name",null,60,"dewasa","luar_rumah"));
        assertEquals(GAGAL,result.status());assertEquals("Synthetic User 1",f.session.getUserName());assertEquals(0,f.db.commits);
        assertEquals(GAGAL,addWater().status());
    }
    @Test void connectionAlreadyInManualModeIsRejectedWithoutTakingOverItsTransaction() throws Exception {
        Connection connection=f.db.connection();connection.setAutoCommit(false);
        var repo=new HidrasiRepository(f.access,()->connection);
        assertEquals(GAGAL,WriteResult.attempt(()->repo.simpan(new Hidrasi(0,1,day,250,""))).status());
        assertEquals(0,f.db.executions);assertEquals(0,f.db.commits);
    }
    @Test void confirmedSleepAndFailedEvaluationArePartialNotFailedSleep() {
        f.db.failExecutionNumber=2;
        var result=f.tidur().catatDenganEvaluasi(1,day,LocalTime.of(23,0),LocalTime.of(2,0),true,f.evaluasi());
        assertEquals(PARSIAL,result.status());assertNotNull(result.value());
        assertEquals(BERHASIL,result.steps().getFirst().status());assertEquals(GAGAL,result.steps().getLast().status());
        assertEquals(1,f.db.table("jadwal_tidur").size());assertTrue(f.db.table("evaluasi_mingguan").isEmpty());
        assertEquals(2,f.db.executions);assertEquals(1,f.db.commits);
    }
    @Test void uncertainSleepDoesNotStartEvaluationOrRetry() {
        f.db.loseCommitAck=true;
        var result=f.tidur().catatDenganEvaluasi(1,day,LocalTime.of(23,0),LocalTime.of(2,0),true,f.evaluasi());
        assertEquals(BELUM_PASTI,result.status());assertEquals(1,f.db.executions);assertTrue(f.db.table("evaluasi_mingguan").isEmpty());
    }
    @Test void uncertainEvaluationRetainsConfirmedSleepAndMarksFollowupUnknown() {
        f.db.beforeCommit=()-> { if(f.db.commits==2)f.db.loseCommitAck=true; };
        var result=f.tidur().catatDenganEvaluasi(1,day,LocalTime.of(23,0),LocalTime.of(2,0),true,f.evaluasi());
        assertEquals(PARSIAL,result.status());assertTrue(result.needsReview());
        assertEquals(BERHASIL,result.steps().getFirst().status());assertEquals(BELUM_PASTI,result.steps().getLast().status());
        assertEquals(1,f.db.table("jadwal_tidur").size());assertEquals(1,f.db.table("evaluasi_mingguan").size());
    }
    @Test void validationAndOwnershipRemainRejectionsNotWriteResults() {
        assertThrows(SecurityException.class,()->WriteResult.attempt(()->{f.kebiasaan().tambahHidrasi(2,day,250,"");return null;}));
        assertThrows(IllegalArgumentException.class,()->WriteResult.attempt(()->{f.kebiasaan().tambahHidrasi(1,day,2,"");return null;}));
        assertEquals(0,f.db.opens);
    }
    @Test void changingIdentityDuringSqlStillCommitsForOriginalOwnerWithoutUpdatingReplacementSnapshot() throws Exception {
        var b=f.db.snapshot("pengguna").get(1);
        f.db.afterWrite=()->{try{f.loginB();}catch(SQLException e){throw new AssertionError(e);}};
        assertThrows(SecurityException.class,()->f.profiles().updateProfil(1,"A Committed",null,60,"dewasa","luar_rumah"));
        assertEquals(1,f.db.commits);assertEquals("A Committed",f.db.table("pengguna").getFirst().get("nama"));
        assertEquals(b,f.db.table("pengguna").get(1));assertEquals(2,f.session.getUserId());assertEquals("Synthetic User 2",f.session.getUserName());
    }
    @Test void committedRegistrationWithCleanupFailureCanAuthenticateOnlyItsCreatedIdentity() throws Exception {
        f.db.afterCommit=()->{f.db.failStatementClose=true;f.db.failConnectionClose=true;};
        var registered=new AuthService(new PenggunaRepository(f.db::connection),f.session)
            .register("New Synthetic","new@example.test","Synthetic New Secret","Synthetic New Secret",true);
        assertEquals(1,f.db.commits);assertEquals(registered.getId(),f.session.getUserId());assertEquals("new@example.test",f.session.getUserEmail());
    }
    @Test void unconfirmedRegistrationCannotCreateASessionEvenIfAccountWasStored() {
        f.db.loseCommitAck=true;long generation=f.session.getGeneration();
        var error=assertThrows(WriteException.class,()->new AuthService(new PenggunaRepository(f.db::connection),f.session)
            .register("New Synthetic","new@example.test","Synthetic New Secret","Synthetic New Secret",true));
        assertEquals(BELUM_PASTI,error.result().status());assertEquals(3,f.db.table("pengguna").size());
        assertEquals(1,f.session.getUserId());assertEquals(generation,f.session.getGeneration());
    }
    @Test void registrationCommitDoesNotOverwriteASessionChangedWhileSqlWasRunning() {
        f.db.beforeCommit=()->{try{f.loginB();}catch(SQLException e){throw new AssertionError(e);}};
        assertThrows(SecurityException.class,()->new AuthService(new PenggunaRepository(f.db::connection),f.session)
            .register("New Synthetic","new@example.test","Synthetic New Secret","Synthetic New Secret",true));
        assertEquals(3,f.db.table("pengguna").size());assertEquals(2,f.session.getUserId());assertEquals("b@example.test",f.session.getUserEmail());
    }
    @Test void partialChallengeAssignmentIsNotAutomaticallyRepeatedAcrossViews() throws Exception {
        var catalog=new TantanganRepository() {
            public List<Tantangan> getByFilter(String a,String b,int c) { return List.of(
                new Tantangan(10,"Synthetic 1","Fixture","hidrasi",5,"semua","semua"),
                new Tantangan(11,"Synthetic 2","Fixture","hidrasi",5,"semua","semua")); }
        };
        var first=new TantanganService(f.access,catalog,new RiwayatTantanganRepository(f.access,f.db::connection));
        f.db.failExecutionNumber=2;
        var failure=assertThrows(WriteException.class,()->first.pastikanTantanganHariIni(f.session.requireActiveUser(),day));
        assertEquals(PARSIAL,failure.result().status());assertEquals(1,f.db.table("riwayat_tantangan").size());
        var anotherScope=new SessionAccess(f.session);
        var another=new TantanganService(anotherScope,catalog,new RiwayatTantanganRepository(anotherScope,f.db::connection));
        assertEquals(1,another.pastikanTantanganHariIni(f.session.requireActiveUser(),day).size());
        assertEquals(2,f.db.executions); // second view performs read-only review, no automatic retry
    }
    @Test void passwordSnapshotIsUpdatedOnlyAfterConfirmedCommit() throws Exception {
        String before=f.session.getActiveUser().getPasswordHash();
        String changed=org.mindrot.jbcrypt.BCrypt.hashpw("Synthetic replacement",org.mindrot.jbcrypt.BCrypt.gensalt(4));
        f.db.beforeCommit=()->assertEquals(before,f.session.getActiveUser().getPasswordHash());
        assertTrue(f.profiles().updatePassword(1,changed));
        assertEquals(changed,f.session.getActiveUser().getPasswordHash());
    }
    @Test void ordinaryProfileReadAfterUncertaintyIsNotAWriteReceiptOrRetry() throws Exception {
        f.db.loseCommitAck=true;
        var result=WriteResult.attempt(()->f.profiles().updateProfil(1,"Stored but unconfirmed",null,60,"dewasa","luar_rumah"));
        assertEquals(BELUM_PASTI,result.status());assertEquals("Synthetic User 1",f.session.getUserName());
        int writes=f.db.executions;
        assertEquals("Stored but unconfirmed",f.profiles().muatProfilTersimpan().getNama());
        assertEquals(writes,f.db.executions);assertEquals(BELUM_PASTI,result.status());
    }
    @Test void lateLoginLookupCannotReplaceNewerSessionIdentity() throws Exception {
        var delayed=new AccountRepository() {
            public Pengguna findByEmail(String email) throws SQLException { var p=f.accounts.findByEmail(email);f.loginB();return p; }
            public boolean existsByEmail(String email) { return false; }
            public int createPengguna(Pengguna p) { throw new AssertionError("No writes"); }
        };
        assertThrows(SecurityException.class,()->new AuthService(delayed,f.session).login("a@example.test",OwnershipFixtures.SECRET));
        assertEquals(2,f.session.getUserId());assertEquals("b@example.test",f.session.getUserEmail());
    }
}
