package com.sehatharian.context;

import com.sehatharian.model.UserSession;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.util.*;
import java.time.Clock;
import java.util.function.Consumer;

/** One composition root per application. No database, worker or audio is touched until used. */
public final class ApplicationContext implements AutoCloseable {
    private final UserSession session;
    private final AuthService authService;
    private final PenggunaService penggunaService;
    private final SceneNavigator navigator;
    private final SoundEffects sounds;
    private final PengingatTidurService reminders;

    public ApplicationContext(AccountRepository accounts, PenggunaRepository profiles) {
        this(accounts, profiles, (java.util.function.Function<UserSession, SleepScheduleProvider>) SqlSleepScheduleProvider::new, ReminderScheduler.worker(),
            Clock.system(PengingatTidurService.ZONE), javafx.application.Platform::runLater,
            new AudioPlayerHelper(com.jpro.webapi.WebAPI.isBrowser()));
    }
    /** Explicit ports for isolated tests; never call bootstrap or an application database in fixtures. */
    public ApplicationContext(AccountRepository accounts, PenggunaRepository profiles,
            SleepScheduleProvider schedules, ReminderScheduler scheduler, Clock clock,
            Consumer<Runnable> ui, AudioOutput output) {
        this(accounts, profiles, (java.util.function.Function<UserSession, SleepScheduleProvider>) ignored -> java.util.Objects.requireNonNull(schedules), scheduler, clock, ui, output);
    }
    private ApplicationContext(AccountRepository accounts, PenggunaRepository profiles,
            java.util.function.Function<UserSession, SleepScheduleProvider> schedules,
            ReminderScheduler scheduler, Clock clock, Consumer<Runnable> ui, AudioOutput output) {
        session = new UserSession();
        authService = new AuthService(accounts, session);
        penggunaService = new PenggunaService(profiles, session);
        sounds = new SoundEffects(session, output);
        reminders = new PengingatTidurService(session, schedules.apply(session), scheduler, clock, ui, sounds);
        navigator = new SceneNavigator(session, authService, penggunaService, reminders, sounds);
    }
    public UserSession session() { return session; }
    public AuthService authService() { return authService; }
    public PenggunaService penggunaService() { return penggunaService; }
    public SceneNavigator navigator() { return navigator; }
    public SoundEffects sounds() { return sounds; }
    public PengingatTidurService reminders() { return reminders; }
    @Override public void close() { navigator.dispose(); }
}
