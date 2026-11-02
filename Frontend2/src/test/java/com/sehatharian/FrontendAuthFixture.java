package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.context.ApplicationContext;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.repository.AccountRepository;
import com.sehatharian.service.AuthService;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;

/** Test-only account access. No SQL or seeded application account. */
final class FrontendAuthFixture {
    private static final String SECRET = "Frontend Fixture Secret";
    private static final String HASH = BCrypt.hashpw(SECRET, BCrypt.gensalt(4));

    static void register(UserSession session, String name, String email) throws SQLException {
        new AuthService(accounts(name, email), session).register(name, email, SECRET, SECRET, true);
    }
    static void login(UserSession session, String name, String email) throws SQLException {
        new AuthService(accounts(name, email), session).login(email, SECRET);
    }
    static ApplicationContext context() {
        return isolatedContext(accounts("Fixture User", "fixture@example.test"));
    }

    static ApplicationContext isolatedContext(AccountRepository accounts) {
        return new ApplicationContext(accounts, noDatabaseProfiles(), id -> null,
            new ReminderTestPorts.Scheduler(), java.time.Clock.systemUTC(), Runnable::run,
            new ReminderTestPorts.Output());
    }

    static PenggunaRepository noDatabaseProfiles() {
        return new PenggunaRepository(() -> { throw new AssertionError("Fixture must not open SQL/network"); }) {
            @Override public Pengguna getById(int id, com.sehatharian.model.SessionAccess access) { throw new AssertionError("No profile SQL lookup in scoped test"); }
            @Override public boolean updatePengguna(Pengguna p, com.sehatharian.model.SessionAccess access) { throw new AssertionError("No profile writes in scoped test"); }
            @Override public boolean updateOnboarding(int id, int minutes, String context, String age, boolean done, com.sehatharian.model.SessionAccess access) {
                throw new AssertionError("No onboarding SQL writes in scoped test");
            }
            @Override public boolean updatePassword(int id, String hash, com.sehatharian.model.SessionAccess access) { throw new AssertionError("No account SQL writes in scoped test"); }
        };
    }

    private static AccountRepository accounts(String name, String email) {
        return new AccountRepository() {
            public Pengguna findByEmail(String identity) {
                return identity.equals(email) ? new Pengguna(91, name, email, HASH, null, "dewasa", "luar_rumah", 45, true) : null;
            }
            public boolean existsByEmail(String identity) { return false; }
            public int createPengguna(Pengguna p) { return 91; }
        };
    }
}
