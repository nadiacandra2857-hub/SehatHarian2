package com.sehatharian.storage;

import java.sql.SQLException;

/** Safe bridge for existing int/boolean/void APIs; never exposes the raw SQL exception. */
public final class WriteException extends SQLException {
    private final WriteResult<?> result;
    public WriteException(WriteResult<?> result) { super(result.message()); this.result = result; }
    public WriteResult<?> result() { return result; }
}
