package com.sehatharian.repository;

import com.sehatharian.model.Pengguna;
import java.sql.SQLException;

/** Account access for normal authentication. Never falls back to demo/memory data. */
public interface AccountRepository {
    Pengguna findByEmail(String email) throws SQLException;
    boolean existsByEmail(String email) throws SQLException;
    /** Returns a positive database ID only after the account has been committed. */
    int createPengguna(Pengguna pengguna) throws SQLException;
}
