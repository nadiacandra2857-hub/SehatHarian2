package com.sehatharian;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.AuthService;
import com.sehatharian.service.PenggunaService;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.util.PasswordUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private final UserSession session = new UserSession();
    private AuthFixtures.Accounts accounts;
    private AuthService auth;

    @BeforeEach void setUp() { session.logout(); accounts = new AuthFixtures.Accounts(); auth = new AuthService(accounts, session); }
    @AfterEach void tearDown() { session.logout(); }

    @Test void adminRoleRequiresVerifiedLoginAndEndsOnLogout() throws SQLException {
        accounts.rows.get(AuthFixtures.EMAIL).setRole("admin");
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        assertTrue(session.isAdmin());
        assertTrue(session.getActiveUser().isAdmin());
        session.logout();
        assertFalse(session.isAdmin());
    }

    @Test void profileRefreshCannotPromoteSession() throws SQLException {
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        Pengguna profile = session.getActiveUser();
        profile.setRole("admin");
        session.refreshCurrentUser(profile);
        assertFalse(session.isAdmin());
        assertFalse(session.getActiveUser().isAdmin());
    }

    @Test void testRegisterSuccess() throws SQLException {
        Pengguna p = auth.register("New User", " New@Example.Test ", "Register Secret 9", "Register Secret 9", true);
        assertSame(accounts.created, p);
        assertEquals(38, p.getId());
        assertEquals("new@example.test", p.getEmail());
        assertTrue(PasswordUtil.checkPassword("Register Secret 9", p.getPasswordHash()));
        assertFalse(p.isOnboardingSelesai());
        assertEquals(p.getId(), session.getUserId());
        assertTrue(session.isLoggedIn());
        assertEquals("user", p.getRole());
        assertFalse(session.isAdmin());
    }
    @Test void testRegisterTermsNotAgreed() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", false));
        assertFalse(session.isLoggedIn());
    }
    @Test void testRegisterInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("Al", "new@example.test", "abcdef", "abcdef", true));
    }
    @Test void testRegisterInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("New User", "invalid", "abcdef", "abcdef", true));
    }
    @Test void testRegisterShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("New User", "new@example.test", "12345", "12345", true));
    }
    @Test void testRegisterMismatchedPassword() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("New User", "new@example.test", "abcdef", "ABCDEF", true));
    }
    @Test void testRegisterDuplicateEmail() {
        assertThrows(IllegalStateException.class, () -> auth.register("New User", AuthFixtures.EMAIL, "abcdef", "abcdef", true));
        assertFalse(session.isLoggedIn());
    }
    @Test void testLoginSuccess() throws SQLException {
        Pengguna p = auth.login("  FIXTURE@EXAMPLE.TEST ", AuthFixtures.SECRET);
        assertEquals(37, p.getId());
        assertEquals(AuthFixtures.EMAIL, accounts.queriedEmail);
        assertTrue(session.isLoggedIn());
        assertEquals(p.getId(), session.getUserId());
        assertEquals(p.getEmail(), session.getUserEmail());
        assertEquals(p.getNama(), session.getUserName());
        assertEquals(p.getId(), session.getActiveUser().getId());
    }
    @Test void testLoginWrongPassword() { rejected("Wrong password"); }
    @ParameterizedTest @ValueSource(strings = {"password123", "admin123"})
    void universalPasswordsDoNotOverrideAnotherAccountsHash(String password) { rejected(password); }
    @ParameterizedTest @ValueSource(strings = {"password123", "admin123", "Password123"})
    void matchingHashForPreviouslyUniversalPasswordStillWorks(String password) throws SQLException {
        accounts.rows.put(AuthFixtures.EMAIL, AuthFixtures.account(BCrypt.hashpw(password, BCrypt.gensalt(4))));
        assertNotNull(auth.login(AuthFixtures.EMAIL, password));
        assertTrue(session.isLoggedIn());
    }
    @Test void testLoginUserNotFound() {
        SecurityException missing = assertThrows(SecurityException.class, () -> auth.login("missing@example.test", AuthFixtures.SECRET));
        SecurityException wrong = assertThrows(SecurityException.class, () -> auth.login(AuthFixtures.EMAIL, "incorrect"));
        assertEquals(AuthService.INVALID_CREDENTIALS, missing.getMessage());
        assertEquals(missing.getMessage(), wrong.getMessage());
        assertFalse(session.isLoggedIn());
    }
    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-hash", "$2a$04$short", "$2a$99$....................................................."})
    void absentOrBrokenHashRejectsWithoutAuthenticating(String hash) {
        accounts.rows.put(AuthFixtures.EMAIL, AuthFixtures.account(hash));
        rejected(AuthFixtures.SECRET);
    }
    @ParameterizedTest @ValueSource(strings = {"$2b$", "$2y$", "$2x$"})
    void unsupportedRevisionRejectsEvenWithOtherwiseMatchingHash(String revision) {
        accounts.rows.put(AuthFixtures.EMAIL, AuthFixtures.account(revision + AuthFixtures.HASH.substring(4)));
        rejected(AuthFixtures.SECRET);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "Fixture User", "invalid@", "missing-domain"})
    void invalidIdentityDoesNotReadRepository(String identity) {
        assertThrows(IllegalArgumentException.class, () -> auth.login(identity, AuthFixtures.SECRET));
        assertEquals(0, accounts.reads);
        assertFalse(session.isLoggedIn());
    }
    @ParameterizedTest @NullAndEmptySource
    void absentPasswordDoesNotReadRepository(String password) {
        assertThrows(IllegalArgumentException.class, () -> auth.login(AuthFixtures.EMAIL, password));
        assertEquals(0, accounts.reads);
        assertFalse(session.isLoggedIn());
    }
    @Test void identityNormalizationDoesNotDependOnOperatingSystemLocale() throws SQLException {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertNotNull(auth.login("FIXTURE@EXAMPLE.TEST", AuthFixtures.SECRET));
            assertEquals(AuthFixtures.EMAIL, accounts.queriedEmail);
        } finally { Locale.setDefault(previous); }
    }
    @Test void passwordIsNeverTrimmedOrCaseNormalized() throws SQLException {
        String password = "  Mixed Case Secret  ";
        accounts.rows.put(AuthFixtures.EMAIL, AuthFixtures.account(BCrypt.hashpw(password, BCrypt.gensalt(4))));
        rejected(password.trim());
        rejected(password.toLowerCase(Locale.ROOT));
        assertNotNull(auth.login(AuthFixtures.EMAIL, password));
    }
    @Test void readFailurePropagatesAndCannotAuthenticateDemo() {
        accounts.readFailure = new SQLException("synthetic infrastructure failure");
        assertSame(accounts.readFailure, assertThrows(SQLException.class, () -> auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET)));
        assertFalse(session.isLoggedIn());
        assertNull(session.getActiveUser());
    }
    @Test void failedLoginPreservesExistingVerifiedIdentity() throws SQLException {
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        accounts.rows.put("other@example.test", new Pengguna(99, "Other User", "other@example.test", AuthFixtures.HASH, null, "dewasa", "luar_rumah", 30, true));
        assertThrows(SecurityException.class, () -> auth.login("other@example.test", "incorrect"));
        assertEquals(37, session.getUserId());
        assertEquals(AuthFixtures.EMAIL, session.getUserEmail());
        accounts.readFailure = new SQLException("synthetic failure");
        assertThrows(SQLException.class, () -> auth.login("other@example.test", AuthFixtures.SECRET));
        assertEquals(37, session.getUserId());
    }
    @Test void invalidRepositoryIdentityCannotAuthenticate() {
        accounts.rows.put(AuthFixtures.EMAIL, new Pengguna(0, "Invalid", AuthFixtures.EMAIL, AuthFixtures.HASH, null, "dewasa", "luar_rumah", 30, true));
        rejected(AuthFixtures.SECRET);
        accounts.rows.put(AuthFixtures.EMAIL, new Pengguna(99, "Other", "other@example.test", AuthFixtures.HASH, null, "dewasa", "luar_rumah", 30, true));
        rejected(AuthFixtures.SECRET);
    }
    @Test void testLogoutClearsSession() throws SQLException {
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        auth.logout();
        assertFalse(session.isLoggedIn());
        assertNull(session.getActiveUser());
        assertEquals(0, session.getUserId());
        assertEquals("", session.getUserEmail());
        assertFalse(session.isOnboardingSelesai());
    }
    @Test void registrationReadFailureCannotAuthenticate() {
        accounts.readFailure = new SQLException("synthetic read failure");
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertNull(accounts.created);
        assertFalse(session.isLoggedIn());
    }
    @Test void registrationWriteFailureCannotAuthenticate() {
        accounts.writeFailure = new SQLException("synthetic write failure");
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertFalse(session.isLoggedIn());
        assertNull(session.getActiveUser());
    }
    @ParameterizedTest @ValueSource(ints = {0, -1})
    void registrationRequiresPositiveCommittedId(int id) {
        accounts.createId = id;
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertFalse(session.isLoggedIn());
    }
    @Test void failedRegistrationPreservesPreviousIdentity() throws SQLException {
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        accounts.writeFailure = new SQLException("synthetic failure");
        assertThrows(SQLException.class, () -> auth.register("New User", "new@example.test", "abcdef", "abcdef", true));
        assertEquals(37, session.getUserId());
        assertEquals(AuthFixtures.EMAIL, session.getUserEmail());
    }
    @Test void activeUserLookupDoesNotRecoverDefaultIdentity() throws SQLException {
        PenggunaRepository noFallback = new PenggunaRepository(() -> {
            throw new AssertionError("No database access during active-user lookup");
        }) {
        };
        assertNull(new PenggunaService(noFallback, session).getPenggunaAktif());
        assertFalse(session.isLoggedIn());
        assertThrows(SecurityException.class, () -> session.requireActiveUser());
    }
    @Test void profileRefreshAndMutableCopiesCannotCreateOrReplaceSession() throws SQLException {
        assertThrows(SecurityException.class, () -> session.refreshCurrentUser(AuthFixtures.account(AuthFixtures.HASH)));
        auth.login(AuthFixtures.EMAIL, AuthFixtures.SECRET);
        Pengguna copy = session.getActiveUser(); copy.setId(99);
        assertEquals(37, session.getActiveUser().getId());
        assertThrows(SecurityException.class, () -> session.refreshCurrentUser(copy));
        assertEquals(37, session.getUserId());
    }
    @Test void unverifiedSessionShortcutsAreNotPublicApi() {
        for (var method : UserSession.class.getMethods()) {
            assertFalse(java.util.Set.of("login", "registerNewAccount", "setCurrentUser", "setLoggedIn", "setUserId", "setUserEmail", "setUserName").contains(method.getName()));
        }
        assertEquals(0, AuthService.VerifiedAccount.class.getConstructors().length);
    }
    private void rejected(String password) {
        assertEquals(AuthService.INVALID_CREDENTIALS, assertThrows(SecurityException.class, () -> auth.login(AuthFixtures.EMAIL, password)).getMessage());
        assertFalse(session.isLoggedIn());
        assertNull(session.getActiveUser());
    }
}
