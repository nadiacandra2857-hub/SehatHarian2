package com.sehatharian.repository;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.config.DatabaseConfig;
import java.util.Objects;

/** Shared, used SQL connection seam and verified owner scope; no memory fallback. */
public abstract class OwnedRepository {
    protected final SessionAccess access;
    protected final PenggunaRepository.ConnectionFactory connections;
    protected OwnedRepository(SessionAccess access) { this(access, DatabaseConfig::getConnection); }
    protected OwnedRepository(SessionAccess access, PenggunaRepository.ConnectionFactory connections) {
        this.access = Objects.requireNonNull(access);
        this.connections = Objects.requireNonNull(connections);
    }
    protected java.sql.Connection open() throws java.sql.SQLException { return open(access, connections); }
    public static java.sql.Connection open(SessionAccess access, PenggunaRepository.ConnectionFactory connections) throws java.sql.SQLException {
        access.owner();
        var connection = connections.open();
        try { access.owner(); return connection; }
        catch (RuntimeException e) {
            try { connection.close(); } catch (java.sql.SQLException close) { e.addSuppressed(close); }
            throw e;
        }
    }
}
