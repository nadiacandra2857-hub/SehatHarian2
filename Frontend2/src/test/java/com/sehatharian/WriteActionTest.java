package com.sehatharian;

import com.sehatharian.storage.WriteResult;
import com.sehatharian.util.WriteAction;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.sehatharian.storage.WriteResult.Status.*;

/** Same submit gate as FXML, with fake controls/rendering and manually drained UI queue. */
class WriteActionTest {
    private FrontendOwnershipFixtures f;
    private WriteAction action;
    private final Deque<Runnable> queue=new ArrayDeque<>();
    private final List<String> form=new ArrayList<>();
    private final List<Boolean> busy=new ArrayList<>();
    private WriteResult<?> displayed;
    @BeforeEach void setup() throws Exception { f=new FrontendOwnershipFixtures();action=new WriteAction(f.access,queue::add);form.add("Synthetic input"); }
    private WriteResult<Void> save() { return WriteResult.attempt(()->{f.kebiasaan().tambahHidrasi(1,LocalDate.of(2026,10,31),250,"");return null;}); }
    private void render(WriteResult<?> result) { displayed=result;if(result.confirmed())form.clear(); }
    private void release() { while(!queue.isEmpty())queue.remove().run(); }
    @Test void onlyConfirmedCommitClearsFormAndRestoresControlsAfterEvent() throws Exception {
        f.db.beforeCommit=()->assertEquals(1,form.size());
        assertTrue(action.execute(this::save,this::render,busy::add));
        assertEquals(BERHASIL,displayed.status());assertTrue(form.isEmpty());assertEquals(List.of(true),busy);
        assertTrue(action.isRunning());release();assertFalse(action.isRunning());assertEquals(List.of(true,false),busy);
    }
    @Test void uncertainWriteKeepsInputAndDisallowsResubmitWithoutAutomaticRetry() throws Exception {
        f.db.loseCommitAck=true;
        action.execute(this::save,this::render,busy::add);release();
        assertEquals(BELUM_PASTI,displayed.status());assertEquals(1,form.size());assertTrue(action.reviewRequired());
        assertFalse(action.execute(this::save,this::render,busy::add));
        assertEquals(1,f.db.executions);assertEquals(1,f.db.table("hidrasi").size());
    }
    @Test void acknowledgedRollbackDoesNotClearForm() throws Exception {
        f.db.failAfterExecute=true;
        action.execute(this::save,this::render,busy::add);release();
        assertEquals(GAGAL,displayed.status());assertEquals(1,form.size());assertFalse(action.reviewRequired());assertTrue(f.db.table("hidrasi").isEmpty());
    }
    @Test void nestedAndAlreadyQueuedDoubleClickDoNotExecuteSecondCommand() throws Exception {
        action.execute(()-> {
            assertFalse(action.execute(this::save,this::render,busy::add));
            return save();
        },this::render,busy::add);
        assertFalse(action.execute(this::save,this::render,busy::add));assertEquals(1,f.db.executions);
        release();assertEquals(1,f.db.executions);
    }
    @Test void staleOwnerCannotReceiveResultOrHaveOldControlsReactivated() throws Exception {
        f.db.afterWrite=()->{try{f.loginB();}catch(Exception e){throw new AssertionError(e);}};
        assertThrows(SecurityException.class,()->action.execute(this::save,this::render,busy::add));
        assertNull(displayed);assertEquals(1,form.size());assertEquals(2,f.session.getUserId());assertEquals(1,f.db.commits);
        release();assertEquals(List.of(true),busy);assertFalse(action.isRunning());
    }
    @Test void partialSleepDoesNotClearOrRepeatPrimarySave() throws Exception {
        f.db.failExecutionNumber=2;
        action.execute(()->f.tidur().catatDenganEvaluasi(1,LocalDate.of(2026,10,31),LocalTime.of(23,0),LocalTime.of(2,0),true,f.evaluasi()),this::render,busy::add);
        assertEquals(PARSIAL,displayed.status());assertEquals(1,form.size());assertTrue(action.reviewRequired());release();
        assertFalse(action.execute(this::save,this::render,busy::add));
        assertEquals(1,f.db.table("jadwal_tidur").size());assertEquals(2,f.db.executions);
    }
    @Test void validationRemainsValidationAndReleasesGateWithoutStorageMessage() {
        assertThrows(IllegalArgumentException.class,()->action.execute(()->{throw new IllegalArgumentException("Synthetic validation");},this::render,busy::add));
        release();assertNull(displayed);assertFalse(action.reviewRequired());assertFalse(action.isRunning());assertEquals(0,f.db.executions);
    }
}
