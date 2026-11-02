package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.AuthService;
import org.junit.jupiter.api.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** JDBC proxies only: no DriverManager, database, network, or application fixtures. */
class AccountRepositoryTest {
    private final UserSession session = new UserSession();
    @BeforeEach void clear() { session.logout(); }
    @AfterEach void clean() { session.logout(); }

    @Test void unavailableConnectionCannotBecomeDemoLoginOrRegistration() {
        PenggunaRepository repo = new PenggunaRepository(() -> { throw new SQLException("synthetic unavailable"); });
        AuthService auth = new AuthService(repo, session);
        assertThrows(SQLException.class, () -> auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET));
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertFalse(session.isLoggedIn());
    }
    @Test void emptySqlResultDoesNotReturnDemoAccount() throws SQLException {
        Jdbc jdbc = new Jdbc();
        assertNull(new PenggunaRepository(jdbc::connection).findByEmail(AuthFixtures.EMAIL));
        assertEquals(1, jdbc.events.stream().filter("query"::equals).count());
    }
    @Test void failedQueryPropagatesInsteadOfReturningDemo() {
        Jdbc jdbc = new Jdbc(); jdbc.failQuery = true;
        AuthService auth = new AuthService(new PenggunaRepository(jdbc::connection), session);
        assertThrows(SQLException.class, () -> auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET));
        assertFalse(session.isLoggedIn());
    }
    @Test void sqlAccountIsUsedOnlyAfterHashVerification() throws SQLException {
        Jdbc jdbc = new Jdbc(); jdbc.rows = 1;
        assertEquals(37, new AuthService(new PenggunaRepository(jdbc::connection), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET).getId());
        assertEquals(37, session.getUserId());
    }
    @Test void duplicateEmailRowsRejectAmbiguousIdentity() {
        Jdbc jdbc = new Jdbc(); jdbc.rows = 2;
        assertThrows(SQLException.class, () -> new AuthService(new PenggunaRepository(jdbc::connection), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET));
        assertFalse(session.isLoggedIn());
    }
    @Test void databaseRoleIsLoadedByVerifiedLogin() throws SQLException {
        Jdbc jdbc = new Jdbc(); jdbc.rows = 1; jdbc.role = "admin";
        new AuthService(new PenggunaRepository(jdbc::connection), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        assertTrue(session.isAdmin());
        assertTrue(session.getActiveUser().isAdmin());
    }
    @Test void unknownRoleDoesNotGrantAdmin() throws SQLException {
        Jdbc jdbc = new Jdbc(); jdbc.rows = 1; jdbc.role = "unexpected";
        new AuthService(new PenggunaRepository(jdbc::connection), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        assertFalse(session.isAdmin());
    }
    @Test void missingPasswordColumnIsStorageFailureNotLegacyLogin() {
        Jdbc jdbc = new Jdbc(); jdbc.rows = 1; jdbc.missingHashColumn = true;
        assertThrows(SQLException.class, () -> new AuthService(new PenggunaRepository(jdbc::connection), session).login(AuthFixtures.EMAIL, AuthFixtures.SECRET));
        assertFalse(session.isLoggedIn());
    }
    @Test void registrationCommitsBeforeSessionAndLeavesInputIdAlone() throws SQLException {
        Jdbc jdbc = new Jdbc();
        AuthService auth = new AuthService(new PenggunaRepository(jdbc::connection), session);
        Pengguna p = auth.register("New User", "new@example.test", "abcdef", "abcdef", true);
        assertEquals(88, p.getId());
        assertEquals(List.of("query", "begin", "insert", "key", "commit"), jdbc.events);
        assertEquals(88, session.getUserId());
    }
    @Test void insertFailureRollsBackAndCannotAuthenticate() { failedRegistration("insert"); }
    @Test void missingGeneratedKeyRollsBackAndCannotAuthenticate() { failedRegistration("key"); }
    @Test void commitFailureRollsBackAndCannotAuthenticate() { failedRegistration("commit"); }
    @Test void noAffectedRowRollsBackAndCannotAuthenticate() { failedRegistration("row"); }
    @Test void nonPositiveKeyRollsBackAndCannotAuthenticate() { failedRegistration("id"); }
    @Test void repositoryDoesNotModifyInputBeforeCommit() {
        Jdbc jdbc = new Jdbc(); jdbc.failure = "commit";
        Pengguna p = AuthFixtures.account(AuthFixtures.HASH); p.setId(0);
        assertThrows(SQLException.class, () -> new PenggunaRepository(jdbc::connection).createPengguna(p));
        assertEquals(0, p.getId());
    }
    private void failedRegistration(String failure) {
        Jdbc jdbc = new Jdbc(); jdbc.failure = failure;
        AuthService auth = new AuthService(new PenggunaRepository(jdbc::connection), session);
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertFalse(session.isLoggedIn());
        assertNull(session.getActiveUser());
        assertTrue(jdbc.events.contains("rollback"));
    }

    private final class Jdbc {
        String role = "user";
        final List<String> events = new ArrayList<>();
        String failure = "";
        boolean failQuery;
        boolean missingHashColumn;
        int rows;
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class}, (p,m,a) -> {
                switch (m.getName()) {
                    case "prepareStatement": return statement();
                    case "getAutoCommit": return true;
                    case "getWarnings": return null;
                    case "setAutoCommit": if (!(Boolean)a[0]) events.add("begin"); return null;
                    case "commit":
                        // If service ever authenticates before commit this assertion fails.
                        assertFalse(session.isLoggedIn());
                        events.add("commit"); if (failure.equals("commit")) throw new SQLException("synthetic commit failure"); return null;
                    case "rollback": events.add("rollback"); return null;
                    case "close": return null;
                    default: throw new AssertionError("Unexpected connection call: " + m.getName());
                }
            });
        }
        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class[]{PreparedStatement.class}, (p,m,a) -> {
                if (m.getName().startsWith("set") || m.getName().equals("close")) return null;
                switch (m.getName()) {
                    case "executeQuery": events.add("query"); if (failQuery) throw new SQLException("synthetic query failure"); return result(false);
                    case "executeUpdate": events.add("insert"); if (failure.equals("insert")) throw new SQLException("synthetic insert failure"); return failure.equals("row") ? 0 : 1;
                    case "getGeneratedKeys": events.add("key"); return result(true);
                    default: throw new AssertionError("Unexpected statement call: " + m.getName());
                }
            });
        }
        ResultSet result(boolean key) {
            int[] cursor = {0};
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class[]{ResultSet.class}, (p,m,a) -> {
                switch (m.getName()) {
                    case "next": return ++cursor[0] <= (key ? (failure.equals("key") ? 0 : 1) : rows);
                    case "getInt": return key ? (failure.equals("id") ? 0 : 88) : (a[0].equals("id") ? 37 : 30);
                    case "getString":
                        if (a[0].equals("password_hash") && missingHashColumn) throw new SQLException("synthetic missing column");
                        return switch ((String)a[0]) {
                            case "email" -> AuthFixtures.EMAIL;
                            case "role" -> role;
                            case "password_hash" -> AuthFixtures.HASH;
                            case "golongan_usia" -> "dewasa";
                            case "golongan_kesibukan" -> "luar_rumah";
                            default -> "Fixture User";
                        };
                    case "getBoolean": return true;
                    case "getDate": case "getTimestamp": case "close": return null;
                    default: throw new AssertionError("Unexpected result call: " + m.getName());
                }
            });
        }
    }
}
