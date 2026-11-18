package com.sehatharian.storage;

import java.sql.SQLException;

/** Safe bridge for existing int/boolean/void APIs; never exposes the raw SQL exception. */
@SuppressWarnings("serial")
public final class WriteException extends SQLException {
    private static final long serialVersionUID = 1L;
    private final transient WriteResult<?> result;
    public WriteException(WriteResult<?> result) { super(result.message()); this.result = result; }
    public WriteResult<?> result() { return result; }
}
