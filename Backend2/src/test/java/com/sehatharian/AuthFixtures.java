package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.repository.AccountRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

final class AuthFixtures {
    static final String EMAIL = "fixture@example.test";
    static final String SECRET = "Fixture Secret 9";
    static final String HASH = BCrypt.hashpw(SECRET, BCrypt.gensalt(4));

    static Pengguna account(String hash) {
        return new Pengguna(37, "Fixture User", EMAIL, hash, null, "dewasa", "luar_rumah", 30, true);
    }

    static class Accounts implements AccountRepository {
        final Map<String, Pengguna> rows = new HashMap<>();
        SQLException readFailure;
        SQLException writeFailure;
        String queriedEmail;
        int createId = 38;
        int reads;
        Pengguna created;

        Accounts() { rows.put(EMAIL, account(HASH)); }
        public Pengguna findByEmail(String email) throws SQLException {
            reads++;
            queriedEmail = email;
            if (readFailure != null) throw readFailure;
            return rows.get(email);
        }
        public boolean existsByEmail(String email) throws SQLException { return findByEmail(email) != null; }
        public int createPengguna(Pengguna p) throws SQLException {
            if (writeFailure != null) throw writeFailure;
            if (createId > 0) { created = p; rows.put(p.getEmail(), p); }
            return createId;
        }
    }
}
