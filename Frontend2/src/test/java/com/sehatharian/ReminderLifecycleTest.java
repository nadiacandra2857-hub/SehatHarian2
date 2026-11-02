package com.sehatharian;

import com.sehatharian.context.ApplicationContext;
import com.sehatharian.controller.*;
import com.sehatharian.model.Pengguna;
import com.sehatharian.repository.AccountRepository;
import com.sehatharian.service.SleepScheduleProvider;
import com.sehatharian.util.*;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real BCrypt, two actual contexts, deterministic loads/ticks/UI queue and synthetic SQL port. */
class ReminderLifecycleTest {
    private static final String PASSWORD = "Synthetic lifecycle password";
    private static final String HASH = BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4));
    private static final SleepScheduleProvider.Schedule MIDNIGHT = new SleepScheduleProvider.Schedule(LocalTime.MIDNIGHT, true);
    private final List<Harness> harnesses = new ArrayList<>();
    private static final class Harness implements AutoCloseable {
        final ReminderTestPorts.Output output = new ReminderTestPorts.Output();
        final ReminderTestPorts.Scheduler scheduler = new ReminderTestPorts.Scheduler();
        final ReminderTestPorts.Time time = new ReminderTestPorts.Time();
        final Deque<Runnable> ui = new ArrayDeque<>();
        final List<String> messages = new ArrayList<>();
        final List<Integer> requested = new ArrayList<>();
        SleepScheduleProvider provider = id -> MIDNIGHT;
        final ApplicationContext context;
        Harness() {
            AccountRepository accounts = new AccountRepository() {
                public Pengguna findByEmail(String email) {
                    int id = email.equals("a@example.test") ? 101 : email.equals("b@example.test") ? 202 : 0;
                    return id == 0 ? null : new Pengguna(id, "Synthetic", email, HASH, null, "dewasa", "luar_rumah", 30, true);
                }
                public boolean existsByEmail(String email) { return findByEmail(email) != null; }
                public int createPengguna(Pengguna user) { throw new AssertionError("No writes in lifecycle tests"); }
            };
            context = new ApplicationContext(accounts, FrontendAuthFixture.noDatabaseProfiles(),
                id -> { requested.add(id); return provider.latest(id); }, scheduler, time, ui::add, output);
        }
        void login(String email) throws Exception { context.authService().login(email, PASSWORD); }
        void start() throws Exception {
            login("a@example.test");
            context.reminders().subscribe(messages::add);
            scheduler.load(); flush(); messages.clear();
        }
        void flush() { while (!ui.isEmpty()) ui.remove().run(); }
        public void close() { context.close(); }
    }
    private Harness create() { var h = new Harness(); harnesses.add(h); return h; }
    @AfterEach void cleanup() { harnesses.forEach(Harness::close); }

    @Test void reminderDependenciesAndSchedulesAreIsolated() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        assertNotSame(a.context.reminders(), b.context.reminders());
        a.context.reminders().setAktif(false); a.context.sounds().setAktif(false);
        assertTrue(b.context.reminders().isAktif()); assertTrue(b.context.sounds().isAktif());
        a.context.reminders().reset();
        assertNull(a.context.reminders().getTargetJamTidur());
        assertEquals(LocalTime.MIDNIGHT, b.context.reminders().getTargetJamTidur());
        b.scheduler.tick(); b.flush();
        assertEquals(1, b.output.plays); assertEquals(0, a.output.plays);
    }
    @Test void logoutOnlyStopsOwnOutputAndLeavesOtherWorkerAndListener() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        int bStops = b.output.stops; a.context.navigator().logout();
        assertTrue(a.output.stops > 0); assertEquals(bStops, b.output.stops);
        assertEquals(0, a.scheduler.liveTasks()); assertEquals(1, b.scheduler.liveTasks());
        b.scheduler.tick(); b.flush(); assertEquals(1, b.output.plays); assertFalse(b.messages.isEmpty());
    }
    @Test void closeOnlyDisposesOwnedResourcesAndIsIdempotent() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        int bStops = b.output.stops; a.close(); a.close();
        assertEquals(1, a.output.closes); assertEquals(1, a.scheduler.closes);
        assertEquals(bStops, b.output.stops); assertEquals(0, b.output.closes);
        b.scheduler.tick(); b.flush(); assertEquals(1, b.output.plays);
    }
    @Test void queuedUiBeforeLogoutDoesNotPlayOrNotify() throws Exception {
        var a = create(); a.start(); a.scheduler.tick(); assertFalse(a.ui.isEmpty());
        a.context.navigator().logout(); a.flush();
        assertEquals(0, a.output.plays); assertTrue(a.messages.isEmpty());
    }
    @Test void queuedUiBeforeIdentityReplacementDoesNotReachNewSubscription() throws Exception {
        var a = create(); a.start(); a.scheduler.tick();
        a.login("b@example.test"); a.context.reminders().subscribe(a.messages::add);
        a.flush(); assertEquals(0, a.output.plays); assertTrue(a.messages.isEmpty());
    }
    @Test void lateLoadCannotInstallOldOwnersSchedule() throws Exception {
        var a = create();
        a.provider = id -> {
            if (id == 101) { a.login("b@example.test"); return MIDNIGHT; }
            return new SleepScheduleProvider.Schedule(LocalTime.of(22, 0), false);
        };
        a.login("a@example.test"); a.scheduler.load();
        assertEquals(List.of(101, 202), a.requested);
        assertEquals(202, a.context.session().getUserId());
        assertEquals(LocalTime.of(22, 0), a.context.reminders().getTargetJamTidur());
        assertFalse(a.context.reminders().isAktif()); assertEquals(1, a.scheduler.liveTasks());
    }
    @Test void staleQueuedLoadIsRejectedBeforeAccessingProvider() throws Exception {
        var a = create(); a.login("a@example.test"); a.login("b@example.test"); a.scheduler.load();
        assertEquals(List.of(202), a.requested); assertEquals(1, a.scheduler.liveTasks());
    }
    @Test void repeatedLoginDoesNotDuplicateWorkerOrOldListener() throws Exception {
        var a = create(); a.start(); int[] oldCalls = {0}; a.context.reminders().subscribe(m -> oldCalls[0]++);
        a.login("a@example.test"); a.login("a@example.test");
        a.context.reminders().subscribe(a.messages::add); a.scheduler.load(); a.flush(); a.messages.clear();
        assertEquals(1, a.scheduler.liveTasks()); a.scheduler.tick(); a.flush();
        assertEquals(0, oldCalls[0]); assertEquals(1, a.messages.size()); assertEquals(1, a.output.plays);
    }
    @Test void resetIsReusableButDisposeDoesNotRestart() throws Exception {
        var a = create(); a.start(); a.context.reminders().reset();
        assertEquals(0, a.scheduler.liveTasks());
        a.context.reminders().subscribe(a.messages::add); a.context.reminders().reloadSchedule();
        a.scheduler.load(); a.flush(); a.scheduler.tick(); a.flush(); assertEquals(1, a.output.plays);
        a.context.reminders().close(); a.context.reminders().close();
        a.context.reminders().reloadSchedule(); assertEquals(0, a.scheduler.liveTasks());
        assertThrows(IllegalStateException.class, () -> a.context.reminders().subscribe(m -> {}));
    }
    @Test void emptyScheduleClearsOldTargetWithoutDemo() throws Exception {
        var a = create(); a.start(); a.provider = id -> null;
        a.context.reminders().reloadSchedule(); assertNull(a.context.reminders().getTargetJamTidur());
        a.scheduler.load(); a.scheduler.tick(); a.flush();
        assertNull(a.context.reminders().getTargetJamTidur()); assertEquals(0, a.output.plays);
        assertEquals(0, a.scheduler.liveTasks()); assertTrue(a.context.reminders().getStatus().contains("Belum ada"));
    }
    @Test void failedScheduleClearsOldTargetAndShowsUnavailable() throws Exception {
        var a = create(); a.start(); a.provider = id -> { throw new java.sql.SQLException("Synthetic failure"); };
        a.context.reminders().reloadSchedule(); a.scheduler.load(); a.scheduler.tick(); a.flush();
        assertNull(a.context.reminders().getTargetJamTidur()); assertEquals(0, a.output.plays);
        assertTrue(a.context.reminders().getStatus().contains("tidak tersedia"));
        assertTrue(a.messages.stream().anyMatch(m -> m.contains("tidak tersedia")));
    }
    @Test void datedMidnightOccurrenceFiresOnceAcrossDateChange() throws Exception {
        var a = create(); a.start(); a.scheduler.tick(); a.flush();
        a.time.at("2026-10-31T17:00:05Z"); a.scheduler.tick(); a.flush();
        assertEquals(1, a.output.plays); assertEquals(1, a.messages.size());
        a.time.at("2026-11-01T16:59:55Z"); a.scheduler.tick(); a.flush(); assertEquals(2, a.output.plays);
    }
    @Test void notificationAndAlarmWindowsKeepExistingToleranceInExplicitJakartaZone() throws Exception {
        var a = create(); a.start();
        a.time.at("2026-10-31T16:49:29Z"); a.scheduler.tick(); a.flush(); assertTrue(a.messages.isEmpty());
        a.time.at("2026-10-31T16:49:30Z"); a.scheduler.tick(); a.flush(); assertEquals(1, a.messages.size());
        a.time.at("2026-10-31T16:50:30Z"); a.scheduler.tick(); a.flush(); assertEquals(1, a.messages.size());
        assertTrue(a.messages.getFirst().contains("WIB")); assertEquals(0, a.output.plays);
        a.time.at("2026-10-31T16:59:49Z"); a.scheduler.tick(); a.flush(); assertEquals(0, a.output.plays);
        a.time.at("2026-10-31T16:59:50Z"); a.scheduler.tick(); a.flush(); assertEquals(1, a.output.plays);
    }
    @Test void exceptionInOneCheckDoesNotKillLaterTicks() throws Exception {
        var a = create(); a.start(); a.time.failNext = true;
        assertDoesNotThrow(a.scheduler::tick); assertEquals(1, a.scheduler.liveTasks());
        a.scheduler.tick(); a.flush(); assertEquals(1, a.output.plays);
    }
    @Test void releasedViewCannotReceiveQueuedBannerOrAudio() throws Exception {
        var a = create(); a.start(); a.context.reminders().reset();
        Runnable detach = a.context.reminders().subscribe(a.messages::add);
        a.context.reminders().reloadSchedule(); a.scheduler.load(); a.flush(); a.messages.clear();
        a.scheduler.tick(); detach.run(); detach.run(); a.flush();
        assertEquals(0, a.output.plays); assertTrue(a.messages.isEmpty());
        assertEquals(1, a.scheduler.liveTasks()); // releasing a view does not kill session reminder
    }
    @Test void queuedAlarmIsInvalidatedByPreferenceToggleAndCancelledOldWorkerCannotRun() throws Exception {
        var a = create(); a.start(); a.scheduler.tick();
        a.context.reminders().setAktif(false); a.flush(); assertEquals(0, a.output.plays);
        Runnable oldTick = a.scheduler.tasks.getFirst().work;
        a.context.reminders().reloadSchedule(); a.scheduler.load(); a.flush(); a.messages.clear();
        oldTick.run(); a.flush(); assertEquals(0, a.output.plays); assertTrue(a.messages.isEmpty());
    }
    @Test void viewEffectsRejectReleaseAndOldGenerationAndMuteStopsOnlyOwnedOutput() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        var view = a.context.sounds().forView(); view.play(SoundEffects.Efek.AIR); assertEquals(1, a.output.plays);
        view.close(); view.play(SoundEffects.Efek.AIR); assertEquals(1, a.output.plays);
        var old = a.context.sounds().forView(); a.login("b@example.test"); old.play(SoundEffects.Efek.KLIK);
        assertEquals(1, a.output.plays); int bStops = b.output.stops;
        a.context.sounds().setAktif(false); assertEquals(bStops, b.output.stops);
        assertTrue(b.context.sounds().isAktif());
    }
    @Test void cacheEvictionClosesViewWithoutAffectingOtherContext() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        var cache = new PrivatePageCache<AutoCloseable>(a.context.session());
        int[] closed = {0}; cache.put("private", () -> closed[0]++);
        a.context.navigator().logout(); assertEquals(1, closed[0]); assertEquals(1, b.scheduler.liveTasks()); cache.close();
    }
    @Test void productionFactoryInjectsOwnRemindersAndSoundAndLogoutUsesOwnNavigator() throws Exception {
        var a = create(); var b = create(); a.start(); b.start();
        var factory = a.context.navigator().newLoader(SceneNavigator.Page.MAIN.path()).getControllerFactory();
        var shell = (MainLayoutController) factory.call(MainLayoutController.class);
        var field = MainLayoutController.class.getDeclaredField("reminders"); field.setAccessible(true);
        assertSame(a.context.reminders(), field.get(shell));
        assertThrows(NullPointerException.class, () -> new MainLayoutController(a.context.session(), a.context.navigator(), null, a.context.sounds()));
        shell.handleLogout(); assertFalse(a.context.session().isLoggedIn()); assertTrue(b.context.session().isLoggedIn());
        shell.close();
    }
    @Test void togglingDuringLoadPreservesChoiceWithoutDiscardingTheSchedule() throws Exception {
        var a = create(); a.login("a@example.test");
        a.context.reminders().setAktif(false); a.scheduler.load();
        assertEquals(LocalTime.MIDNIGHT, a.context.reminders().getTargetJamTidur());
        assertFalse(a.context.reminders().isAktif()); assertEquals(1, a.scheduler.liveTasks());
    }
    @Test void sameScheduleReloadDoesNotRepeatTheSameDatedAlarm() throws Exception {
        var a = create(); a.start(); a.scheduler.tick(); a.flush();
        a.context.reminders().reloadSchedule(); a.scheduler.load(); a.flush();
        a.scheduler.tick(); a.flush(); assertEquals(1, a.output.plays);
    }
    @Test void terminalCloseRejectsAlreadyQueuedUiAndCancelledTick() throws Exception {
        var a = create(); a.start(); a.scheduler.tick();
        var old = a.scheduler.tasks.getFirst().work;
        a.close(); old.run(); a.flush();
        assertTrue(a.messages.isEmpty()); assertEquals(0, a.output.plays);
        assertEquals(1, a.output.closes);
    }
    @Test void identityReplacementResetsPreferenceAndDoesNotReuseFormerTarget() throws Exception {
        var a = create(); a.start();
        a.context.sounds().setAktif(false); a.context.reminders().setAktif(false);
        a.provider = id -> null; a.login("b@example.test");
        assertNull(a.context.reminders().getTargetJamTidur());
        assertTrue(a.context.sounds().isAktif()); assertTrue(a.context.reminders().isAktif());
        a.scheduler.load(); assertNull(a.context.reminders().getTargetJamTidur());
        a.scheduler.tick(); a.flush(); assertEquals(0, a.output.plays);
    }
    @Test void releasedShellCannotLogoutOrToggleReplacementSessionsOutput() throws Exception {
        var a = create(); a.start();
        var shell = (MainLayoutController) a.context.navigator().newLoader(SceneNavigator.Page.MAIN.path())
            .getControllerFactory().call(MainLayoutController.class);
        shell.close(); shell.close();
        shell.handleToggleSuara(); shell.handleUjiAlarm(); shell.handleLogout();
        assertTrue(a.context.session().isLoggedIn()); assertTrue(a.context.sounds().isAktif());
        assertEquals(0, a.output.plays);
    }
    @Test void audioFailureDoesNotSuppressVisualNotification() throws Exception {
        var a = create(); a.start(); a.output.failPlay = true;
        a.scheduler.tick(); assertDoesNotThrow(a::flush);
        assertEquals(1, a.messages.size()); assertEquals(1, a.scheduler.liveTasks());
    }
    @Test void listenerDetachedByAnotherListenerInSameDeliveryIsNotCalled() throws Exception {
        var a = create(); a.login("a@example.test");
        Runnable[] detachSecond = {() -> {}};
        a.context.reminders().subscribe(message -> { if (message.startsWith("Waktu tidur")) detachSecond[0].run(); });
        detachSecond[0] = a.context.reminders().subscribe(a.messages::add);
        a.scheduler.load(); a.flush(); a.messages.clear();
        a.scheduler.tick(); a.flush(); assertTrue(a.messages.isEmpty());
    }
    @Test void oldSubscriptionDetachCannotStopReplacementSessionsOutput() throws Exception {
        var a = create(); a.start();
        Runnable stale = a.context.reminders().subscribe(message -> {});
        a.login("b@example.test");
        int stops = a.output.stops; stale.run(); stale.run();
        assertEquals(stops, a.output.stops);
    }
    @Test void browserBackendDoesNotTouchServerAudioEvenForInvalidResource() {
        var output = new AudioPlayerHelper(true);
        assertFalse(output.supportsPlayback());
        assertDoesNotThrow(() -> { output.play(null, 1); output.stop(); output.close(); output.close(); });
    }
}
