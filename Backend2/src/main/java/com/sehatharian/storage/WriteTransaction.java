package com.sehatharian.storage;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.OwnedRepository;
import com.sehatharian.repository.PenggunaRepository.ConnectionFactory;
import java.sql.*;
import java.util.Objects;

/** One owned connection/one repository operation; no nesting, retry, thread/global transaction manager. */
public final class WriteTransaction implements AutoCloseable {
    private final SessionAccess access;
    private final ConnectionFactory connections;
    private Connection connection;
    private boolean manual, executed, commitSent, confirmed, resolved, closed;
    private Object value;
    public WriteTransaction(SessionAccess access, ConnectionFactory connections) {
        this.access = Objects.requireNonNull(access); this.connections = Objects.requireNonNull(connections);
    }
    private WriteTransaction(ConnectionFactory connections) { this.access = null; this.connections = Objects.requireNonNull(connections); }
    /** Anonymous creation remains exclusively behind the existing validated AuthService registration flow. */
    public static WriteTransaction accountCreation(ConnectionFactory connections) { return new WriteTransaction(connections); }
    public Connection open() throws SQLException {
        connection = access == null ? connections.open() : OwnedRepository.open(access, connections);
        // Factory must supply a fresh, owned connection. Never commit a caller's pre-existing transaction.
        if (!connection.getAutoCommit()) throw new SQLException("Koneksi operasi tidak tersedia.");
        connection.setAutoCommit(false);
        manual = true;
        return connection;
    }
    public int execute(PreparedStatement statement) throws SQLException {
        if (access != null) access.owner();
        if (!manual) throw new IllegalStateException("Transaksi belum dimulai.");
        executed = true;
        return statement.executeUpdate(); // Delivery guard is AFTER confirmation, not a claim of rollback.
    }
    public <T> T confirm(T result) throws SQLException {
        commitSent = true;
        connection.commit();
        confirmed = true; resolved = true; value = result;
        return access == null ? result : access.result(result);
    }
    public boolean affected(int rows) throws SQLException {
        if (rows > 0) return confirm(true);
        connection.rollback();
        if (connection.getWarnings() != null) throw new SQLException("Rollback belum dikonfirmasi.");
        resolved = true;
        return access == null ? false : access.result(false);
    }
    @SuppressWarnings("unchecked")
    public <T> T recover(SQLException ignored) throws WriteException {
        if (confirmed) return access == null ? (T)value : access.result((T)value);
        boolean rolledBack = rollback();
        if (commitSent || executed && !rolledBack) throw new WriteException(WriteResult.uncertain());
        throw new WriteException(WriteResult.failed());
    }
    private boolean rollback() {
        if (confirmed || resolved) return resolved;
        if (!manual || connection == null) return !executed;
        try {
            connection.rollback();
            if (connection.getWarnings() != null) return false;
            resolved = true; return true;
        }
        catch (SQLException e) { return false; }
    }
    public void abort() { if (!confirmed) rollback(); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        if (!confirmed && !resolved) rollback();
        if (connection != null) {
            // Switching false -> true can commit. Do not do that after unknown commit/rollback.
            if (manual && resolved && (!commitSent || confirmed)) {
                try { connection.setAutoCommit(true); } catch (SQLException e) { /* confirmation unchanged */ }
            }
            try { connection.close(); } catch (SQLException e) { /* confirmation unchanged; never log raw SQL */ }
        }
    }
}
