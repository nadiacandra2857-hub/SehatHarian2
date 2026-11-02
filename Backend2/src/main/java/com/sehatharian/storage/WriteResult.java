package com.sehatharian.storage;

import com.sehatharian.model.SessionAccess;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Ephemeral confirmation, not an idempotency key or a durable transaction receipt. */
public record WriteResult<T>(Status status, T value, List<Step> steps) {
    public enum Status { BERHASIL, GAGAL, PARSIAL, BELUM_PASTI }
    public record Step(String name, Status status) { public Step { Objects.requireNonNull(name); Objects.requireNonNull(status); } }
    public WriteResult { Objects.requireNonNull(status); steps = List.copyOf(steps); }
    public static <T> WriteResult<T> success(T value) { return new WriteResult<>(Status.BERHASIL,value,List.of()); }
    public static <T> WriteResult<T> failed() { return new WriteResult<>(Status.GAGAL,null,List.of()); }
    public static <T> WriteResult<T> uncertain() { return new WriteResult<>(Status.BELUM_PASTI,null,List.of()); }
    public boolean confirmed() { return status == Status.BERHASIL; }
    public boolean needsReview() { return status == Status.BELUM_PASTI || steps.stream().anyMatch(s -> s.status == Status.BELUM_PASTI); }
    public static <T> WriteResult<T> attempt(SessionAccess.Operation<T> command) {
        try {
            T value = command.run();
            return Boolean.FALSE.equals(value) ? failed() : success(value);
        } catch (WriteException e) { return new WriteResult<>(e.result().status(),null,e.result().steps()); }
        catch (SQLException e) { return uncertain(); } // Unknown legacy/read boundary: never assume no commit.
    }
    public String message() {
        String pending = steps.stream().filter(step -> step.status != Status.BERHASIL).map(Step::name).distinct().collect(java.util.stream.Collectors.joining(", "));
        return switch(status) {
            case BERHASIL -> "Data berhasil disimpan.";
            case GAGAL -> "Data belum tersimpan atau tidak berubah. Isian tetap tersedia.";
            case BELUM_PASTI -> "Data mungkin telah tersimpan. Periksa data tersimpan atau riwayat sebelum mengulang; jangan langsung menyimpan ulang.";
            case PARSIAL -> needsReview()
                ? "Sebagian langkah sudah tersimpan. Langkah belum pasti: " + pending + ". Periksa riwayat sebelum mengulang."
                : "Sebagian langkah sudah tersimpan. Belum selesai: " + pending + ". Jangan ulangi langkah yang sudah tersimpan.";
        };
    }
}
