package com.sehatharian;

import com.sehatharian.context.ApplicationContext;
import com.sehatharian.controller.*;
import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.repository.AccountRepository;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.AuthService;
import com.sehatharian.util.InstanceControllerFactory;
import com.sehatharian.util.PrivatePageCache;
import com.sehatharian.util.SceneNavigator;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Two contexts in one process/classloader. No Platform.startup, Stage, FXML load or JDBC call. */
class InstanceContextTest {
    private static final String SECRET = "Synthetic context secret";
    private static final String HASH = BCrypt.hashpw(SECRET, BCrypt.gensalt(4));
    private ApplicationContext a, b;

    @BeforeEach void setUp() { a = context(111, "a@example.test"); b = context(222, "b@example.test"); }
    @AfterEach void tearDown() { a.navigator().dispose(); b.navigator().dispose(); }

    @Test void contextsStartAnonymousWithDistinctDependencies() {
        assertFalse(a.session().isLoggedIn()); assertFalse(b.session().isLoggedIn());
        assertNotSame(a.session(), b.session());
        assertNotSame(a.authService(), b.authService());
        assertNotSame(a.penggunaService(), b.penggunaService());
        assertNotSame(a.navigator(), b.navigator());
        assertNull(a.navigator().getPrimaryStage()); assertNull(b.navigator().getPrimaryStage());
    }
    @Test void loginAOnlyAuthenticatesA() throws SQLException {
        loginA();
        assertEquals(111, a.session().getUserId());
        assertFalse(b.session().isLoggedIn()); assertNull(b.session().getActiveUser());
        assertNull(b.penggunaService().getPenggunaAktif());
    }
    @Test void loginBDoesNotReplaceA() throws SQLException {
        loginA(); long generationA = a.session().getGeneration(); loginB();
        assertEquals(111, a.session().getUserId()); assertEquals(222, b.session().getUserId());
        assertEquals(generationA, a.session().getGeneration());
    }
    @Test void failedLoginBLeavesAAndPreviouslyVerifiedBUntouched() throws SQLException {
        loginA(); loginB(); long generationA = a.session().getGeneration(); long generationB = b.session().getGeneration();
        assertThrows(SecurityException.class, () -> b.authService().login("b@example.test", "wrong"));
        assertEquals(111, a.session().getUserId()); assertEquals(222, b.session().getUserId());
        assertEquals(generationA, a.session().getGeneration()); assertEquals(generationB, b.session().getGeneration());
    }
    @Test void failedAnonymousBDoesNotAuthenticateEitherContext() {
        assertThrows(SecurityException.class, () -> b.authService().login("missing@example.test", SECRET));
        assertFalse(a.session().isLoggedIn()); assertFalse(b.session().isLoggedIn());
    }
    @Test void registrationTargetsOnlyInjectedSession() throws SQLException {
        a.authService().register("New User", "new@example.test", "New fixture secret", "New fixture secret", true);
        assertTrue(a.session().isLoggedIn()); assertEquals(112, a.session().getUserId());
        assertFalse(b.session().isLoggedIn());
    }
    @Test void snapshotsCannotMutateEitherIdentity() throws SQLException {
        loginA(); loginB();
        Pengguna snapshot = a.session().getActiveUser(); snapshot.setId(222); snapshot.setEmail("b@example.test");
        assertEquals(111, a.session().getActiveUser().getId()); assertEquals("a@example.test", a.session().getUserEmail());
        assertEquals(222, b.session().getActiveUser().getId());
    }
    @Test void refreshCannotSwapOwnersOrRecoverAnAnonymousContext() throws SQLException {
        loginA();
        assertThrows(SecurityException.class, () -> b.session().refreshCurrentUser(a.session().getActiveUser()));
        loginB();
        assertThrows(SecurityException.class, () -> a.session().refreshCurrentUser(b.session().getActiveUser()));
        Pengguna own = a.session().getActiveUser(); own.setNama("Updated A"); a.session().refreshCurrentUser(own);
        assertEquals("Updated A", a.session().getUserName()); assertEquals("User 222", b.session().getUserName());
    }
    @Test void protectedNavigationUsesOnlyItsOwnContext() throws SQLException {
        assertEquals(SceneNavigator.Page.LOGIN, a.navigator().resolveDestination(SceneNavigator.Page.MAIN));
        assertEquals(SceneNavigator.Page.LOGIN, b.navigator().resolveDestination(SceneNavigator.Page.ONBOARDING));
        loginA();
        for (var page : SceneNavigator.Page.values()) {
            assertEquals(page, a.navigator().resolveDestination(page));
            assertEquals(page.isPrivate() ? SceneNavigator.Page.LOGIN : page, b.navigator().resolveDestination(page));
        }
    }
    @Test void loaderUsesScopedFactoryAndDirectPrivateLoadingRejectsAnonymous() throws Exception {
        var loaderA = a.navigator().newLoader(SceneNavigator.Page.LOGIN.path());
        var loaderB = b.navigator().newLoader(SceneNavigator.Page.LOGIN.path());
        assertNotNull(loaderA.getControllerFactory()); assertNotSame(loaderA.getControllerFactory(), loaderB.getControllerFactory());
        Object controllerA = loaderA.getControllerFactory().call(LoginController.class);
        Object controllerB = loaderB.getControllerFactory().call(LoginController.class);
        assertNotSame(controllerA, controllerB);
        assertSame(a.authService(), field(controllerA, "authService")); assertSame(a.navigator(), field(controllerA, "navigator"));
        assertSame(b.authService(), field(controllerB, "authService")); assertSame(b.navigator(), field(controllerB, "navigator"));
        assertThrows(SecurityException.class, () -> a.navigator().newLoader(SceneNavigator.Page.MAIN.path()));
        assertThrows(SecurityException.class, () -> loaderA.getControllerFactory().call(MainLayoutController.class));
    }
    @Test void factoryNeverCreatesMissingContextOrFallsBackToNoArgControllers() throws Exception {
        assertThrows(NullPointerException.class, () -> new LoginController(null, a.navigator()));
        assertThrows(NullPointerException.class, () -> new MainLayoutController(null, a.navigator(), a.reminders(), a.sounds()));
        assertThrows(NullPointerException.class, () -> new AuthService(provider(1, "fixture@example.test"), null));
        assertThrows(NullPointerException.class, () -> new InstanceControllerFactory(a.session(), a.authService(), null, a.navigator(), a.reminders(), a.sounds()));
        loginA();
        var factory = a.navigator().newLoader(SceneNavigator.Page.MAIN.path()).getControllerFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.call(Object.class));
        for (var type : new Class<?>[]{LoginController.class, CreateAccountController.class, MainLayoutController.class, OnboardingController.class}) {
            for (var constructor : type.getConstructors()) assertTrue(constructor.getParameterCount() > 0);
        }
    }
    @Test void controllerAndCacheInstancesDoNotCrossContexts() throws Exception {
        loginA(); loginB();
        Object ca = a.navigator().newLoader(SceneNavigator.Page.MAIN.path()).getControllerFactory().call(MainLayoutController.class);
        Object cb = b.navigator().newLoader(SceneNavigator.Page.MAIN.path()).getControllerFactory().call(MainLayoutController.class);
        assertNotSame(ca, cb); assertSame(a.session(), field(ca, "session")); assertSame(b.session(), field(cb, "session"));
        var cacheA = new PrivatePageCache<Object>(a.session()); var cacheB = new PrivatePageCache<Object>(b.session());
        cacheA.put("shell", ca); cacheB.put("shell", cb);
        assertSame(ca, cacheA.get("shell")); assertSame(cb, cacheB.get("shell"));
        a.authService().logout();
        assertEquals(0, cacheA.size()); assertSame(cb, cacheB.get("shell"));
        assertEquals(222, b.session().getUserId());
        assertThrows(SecurityException.class, () -> cacheA.get("shell"));
        loginA(); assertNull(cacheA.get("shell"));
    }
    @Test void logoutThroughActualControllerClearsOnlyAAndReturnsToPublicDestination() throws Exception {
        loginA(); loginB();
        a.session().completeOnboarding(2, "2 jam", "INDOOR", "LANSIA");
        b.session().completeOnboarding(3, "3 jam", "OUTDOOR", "DEWASA");
        MainLayoutController controller = (MainLayoutController) a.navigator().newLoader(SceneNavigator.Page.MAIN.path())
                .getControllerFactory().call(MainLayoutController.class);
        controller.handleLogout();
        assertFalse(a.session().isLoggedIn()); assertNull(a.session().getActiveUser());
        assertEquals("", a.session().getWaktuLuangLabel()); assertEquals(0, a.navigator().cachedPrivatePageCount());
        assertEquals(SceneNavigator.Page.LANDING, a.navigator().currentPage());
        assertTrue(b.session().isLoggedIn()); assertEquals("3 jam", b.session().getWaktuLuangLabel());
        assertEquals(SceneNavigator.Page.LOGIN, a.navigator().resolveDestination(SceneNavigator.Page.PROFIL));
    }
    @Test void staleShellCannotLoadPrivatePagesOrLogoutAReplacementSession() throws Exception {
        loginA();
        MainLayoutController old = (MainLayoutController) a.navigator().newLoader(SceneNavigator.Page.MAIN.path())
                .getControllerFactory().call(MainLayoutController.class);
        a.navigator().logout();
        var load = MainLayoutController.class.getDeclaredMethod("muatTampilan", String.class, String.class); load.setAccessible(true);
        InvocationTargetException anonymous = assertThrows(InvocationTargetException.class,
                () -> load.invoke(old, SceneNavigator.Page.DASHBOARD.path(), "Dashboard"));
        assertInstanceOf(SecurityException.class, anonymous.getCause());
        loginA(); long generation = a.session().getGeneration(); old.handleLogout();
        assertTrue(a.session().isLoggedIn()); assertEquals(generation, a.session().getGeneration());
        InvocationTargetException stale = assertThrows(InvocationTargetException.class,
                () -> load.invoke(old, SceneNavigator.Page.DASHBOARD.path(), "Dashboard"));
        assertInstanceOf(SecurityException.class, stale.getCause());
    }
    @Test void identityAndStageHaveNoStaticCurrentInstance() {
        for (var field : UserSession.class.getDeclaredFields()) assertFalse(Modifier.isStatic(field.getModifiers()));
        for (var field : SceneNavigator.class.getDeclaredFields()) assertFalse(Modifier.isStatic(field.getModifiers()));
        assertThrows(NoSuchMethodException.class, () -> UserSession.class.getMethod("getInstance"));
    }
    @Test void privateCacheRejectsAnonymousAndInvalidatesOnSuccessfulReauthenticationOnly() throws SQLException {
        var cache = new PrivatePageCache<Object>(a.session()); Object page = new Object();
        assertThrows(SecurityException.class, () -> cache.put("page", page));
        loginA(); cache.put("page", page);
        assertThrows(SecurityException.class, () -> a.authService().login("a@example.test", "wrong"));
        assertSame(page, cache.get("page"));
        loginA(); assertEquals(0, cache.size());
    }

    private void loginA() throws SQLException { a.authService().login("a@example.test", SECRET); }
    private void loginB() throws SQLException { b.authService().login("b@example.test", SECRET); }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static ApplicationContext context(int id, String email) {
        return FrontendAuthFixture.isolatedContext(provider(id, email));
    }
    private static AccountRepository provider(int id, String email) {
        return new AccountRepository() {
            private final Map<String, Pengguna> created = new HashMap<>();
            public Pengguna findByEmail(String key) {
                if (created.containsKey(key)) return created.get(key);
                return email.equals(key) ? new Pengguna(id, "User " + id, email, HASH, null, "dewasa", "luar_rumah", 30, true) : null;
            }
            public boolean existsByEmail(String key) { return findByEmail(key) != null; }
            public int createPengguna(Pengguna p) { created.put(p.getEmail(), p); return id + 1; }
        };
    }
}
