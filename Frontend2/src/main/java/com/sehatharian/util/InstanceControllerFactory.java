package com.sehatharian.util;

import com.sehatharian.controller.*;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.AuthService;
import com.sehatharian.service.PenggunaService;
import javafx.util.Callback;
import java.util.Objects;

/** Explicit constructor wiring for the 13 existing FXML controllers; no reflection/default fallback. */
public final class InstanceControllerFactory implements Callback<Class<?>, Object> {
    private final UserSession session;
    private final AuthService auth;
    private final PenggunaService profiles;
    private final SceneNavigator navigator;
    private final com.sehatharian.service.PengingatTidurService reminders;
    private final SoundEffects sounds;

    public InstanceControllerFactory(UserSession session, AuthService auth,
                                     PenggunaService profiles, SceneNavigator navigator,
                                     com.sehatharian.service.PengingatTidurService reminders, SoundEffects sounds) {
        this.session = Objects.requireNonNull(session);
        this.auth = Objects.requireNonNull(auth);
        this.profiles = Objects.requireNonNull(profiles);
        this.navigator = Objects.requireNonNull(navigator);
        this.reminders = Objects.requireNonNull(reminders);
        this.sounds = Objects.requireNonNull(sounds);
    }

    @Override public Object call(Class<?> type) {
        Objects.requireNonNull(type);
        if (type == LandingPageController.class) return new LandingPageController(navigator);
        if (type == LoginController.class) return new LoginController(auth, navigator);
        if (type == CreateAccountController.class) return new CreateAccountController(session, auth, navigator);
        session.requireActiveUser();
        if (type == OnboardingController.class) return new OnboardingController(session, profiles, navigator);
        if (type == MainLayoutController.class) return new MainLayoutController(session, navigator, reminders, sounds);
        if (type == DashboardController.class) return new DashboardController(session, sounds);
        if (type == TantanganController.class) return new TantanganController(session, sounds);
        if (type == RekomendasiController.class) return new RekomendasiController(session);
        if (type == CatatanKebiasaanController.class) return new CatatanKebiasaanController(session, sounds);
        if (type == JadwalTidurController.class) return new JadwalTidurController(session, reminders);
        if (type == StatistikMingguanController.class) return new StatistikMingguanController(session);
        if (type == EvaluasiMingguanController.class) return new EvaluasiMingguanController(session);
        if (type == ProfilPengaturanController.class) return new ProfilPengaturanController(session, profiles);
        throw new IllegalArgumentException("Controller tidak terdaftar: " + type.getName());
    }
}
