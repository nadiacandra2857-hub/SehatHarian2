package com.sehatharian.util;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.storage.WriteResult;
import javafx.scene.Parent;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One view's submit gate. Read-only navigation/history remain available after an uncertain write. */
public final class WriteAction {
    private final SessionAccess access;
    private final Consumer<Runnable> defer;
    private final AtomicBoolean running = new AtomicBoolean();
    private boolean reviewRequired;
    public WriteAction(SessionAccess access) { this(access, javafx.application.Platform::runLater); }
    public WriteAction(SessionAccess access, Consumer<Runnable> defer) {
        this.access = Objects.requireNonNull(access); this.defer = Objects.requireNonNull(defer);
    }
    public boolean isRunning() { return running.get(); }
    public boolean reviewRequired() { return reviewRequired; }
    /** Headless core used by real FXML submissions and controlled-queue tests. */
    public <T> boolean execute(SessionAccess.Operation<WriteResult<T>> command,
            Consumer<WriteResult<T>> deliver, Consumer<Boolean> busy) throws SQLException {
        access.owner();
        if (reviewRequired || !running.compareAndSet(false,true)) return false;
        try {
            busy.accept(true);
            WriteResult<T> result = command.run();
            access.owner();
            reviewRequired = result.needsReview() || result.status() == WriteResult.Status.PARSIAL;
            deliver.accept(result);
            return true;
        } finally {
            // Keep queued/nested double-clicks locked until the current event/dialog has finished.
            Runnable release = () -> {
                try { access.owner(); busy.accept(false); }
                catch (SecurityException ignored) { /* released/replaced view must not be updated */ }
                finally { running.set(false); }
            };
            try { defer.accept(release); }
            catch (RuntimeException toolkitClosed) { running.set(false); }
        }
    }
    public <T> void submit(Parent root, SessionAccess.Operation<T> command, Runnable confirmed) throws SQLException {
        submitResult(root, () -> WriteResult.attempt(command), result -> {
            if (result.confirmed()) confirmed.run();
            else DialogHelper.showWarning(root, "Hasil Penyimpanan", result.message());
        });
    }
    public <T> void submitResult(Parent root, SessionAccess.Operation<WriteResult<T>> command,
            Consumer<WriteResult<T>> deliver) throws SQLException {
        execute(command, result -> {
            try { deliver.accept(result); }
            catch (SecurityException stale) { throw stale; }
            catch (RuntimeException viewFailure) {
                access.owner();
                DialogHelper.showWarning(root, "Hasil Penyimpanan", result.message() + " Tampilan belum dapat diperbarui; buka riwayat.");
            }
        }, value -> {
            if (root == null) return;
            root.setDisable(value);
            if (!value && reviewRequired) root.lookupAll(".write-command").forEach(node -> node.setDisable(true));
        });
    }
}
