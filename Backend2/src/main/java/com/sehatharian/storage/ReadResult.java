package com.sehatharian.storage;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Origin of one completed read, never a receipt for an earlier write. */
public final class ReadResult<T> {
    public enum Source { DATABASE, DEMO, UNAVAILABLE }
    @FunctionalInterface public interface Query<T> { T read() throws SQLException; }
    private final Source source;
    private final T value;
    private final Instant observedAt;

    private ReadResult(Source source, T value) {
        this.source = source;
        this.value = value;
        this.observedAt = Instant.now();
    }
    public static <T> ReadResult<T> database(Query<T> query) {
        Objects.requireNonNull(query);
        try { return new ReadResult<>(Source.DATABASE, Objects.requireNonNull(query.read())); }
        catch (SQLException e) { return new ReadResult<>(Source.UNAVAILABLE, null); }
    }
    public static <T> ReadResult<T> demo(T value) {
        return new ReadResult<>(Source.DEMO, Objects.requireNonNull(value));
    }
    public Source source() { return source; }
    public Optional<T> value() { return Optional.ofNullable(value); }
    public Instant observedAt() { return observedAt; }
    public String message() {
        return switch (source) {
            case DATABASE -> "Katalog dari database.";
            case DEMO -> "Pratinjau data contoh. Tidak disimpan ke akun Anda.";
            case UNAVAILABLE -> "Katalog gagal dimuat. Silakan coba muat kembali.";
        };
    }
}
