package com.sehatharian.model;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** A command/view is bound to the verified identity and generation when it was created. */
public final class SessionAccess implements AutoCloseable {
    private final UserSession session;
    private final long generation;
    private final int userId;
    private volatile boolean released;
    public SessionAccess(UserSession session) {
        this.session = Objects.requireNonNull(session);
        generation = session.getGeneration();
        userId = session.isLoggedIn() ? session.getUserId() : 0;
    }
    public int owner() {
        if (released || userId <= 0 || !session.isLoggedIn() || generation != session.getGeneration()
                || userId != session.getUserId()) throw new SecurityException("Permintaan sesi tidak berlaku. Silakan masuk kembali.");
        return userId;
    }
    public int owner(int claimed) {
        int actual = owner();
        if (claimed != actual) throw new SecurityException("Data tidak tersedia.");
        return actual;
    }
    public void requireSession(UserSession expected) {
        if (session != expected) throw new SecurityException("Konteks sesi tidak sesuai.");
        owner();
    }
    public Pengguna profile() { owner(); return result(session.requireActiveUser()); }
    public <T> T result(T value) { owner(); return value; }
    public <T> List<T> rows(List<T> values, ToIntFunction<T> ownerOf) {
        int actual = owner();
        if (values == null || values.stream().anyMatch(v -> v == null || ownerOf.applyAsInt(v) != actual))
            throw new SecurityException("Data tidak tersedia.");
        return result(List.copyOf(values));
    }
    public <T> T row(T value, ToIntFunction<T> ownerOf) {
        if (value != null) owner(ownerOf.applyAsInt(value));
        return result(value);
    }
    @FunctionalInterface public interface Operation<T> { T run() throws SQLException; }
    public <T> T call(Operation<T> operation) throws SQLException {
        owner();
        T value = operation.run();
        return result(value);
    }
    public boolean automaticChallengesBlocked(java.time.LocalDate date) { owner(); return session.automaticChallengesBlocked(date); }
    public void blockAutomaticChallenges(java.time.LocalDate date) { owner(); session.blockAutomaticChallenges(date); }
    public void apply(Runnable update) { owner(); update.run(); }
    @Override public void close() { released = true; }
}
