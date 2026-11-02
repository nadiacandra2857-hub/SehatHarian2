package com.sehatharian.util;

import com.sehatharian.model.UserSession;
import java.util.Objects;

/** Preferences and playback are session-local, never JVM/OS-user preferences. */
public final class SoundEffects implements AudioOutput {
    public enum Efek {
        SUKSES("/sounds/success_chime.wav", .55), AIR("/sounds/water_drop.wav", .6),
        KLIK("/sounds/soft_click.wav", .35);
        final String path; final double volume;
        Efek(String path, double volume) { this.path = path; this.volume = volume; }
    }
    private final UserSession session;
    private final AudioOutput backend;
    private final Runnable detach;
    private boolean aktif = true, closed;
    public SoundEffects(UserSession session, AudioOutput backend) {
        this.session = Objects.requireNonNull(session);
        this.backend = Objects.requireNonNull(backend);
        detach = session.onIdentityChanged(this::reset);
    }
    @Override public boolean supportsPlayback() { return backend.supportsPlayback(); }
    public synchronized boolean isAktif() { return aktif; }
    public synchronized void setAktif(boolean enabled) {
        if (closed) return;
        aktif = enabled;
        if (!enabled) stop();
    }
    public synchronized void reset() { if (!closed) { aktif = true; stop(); } }
    @Override public synchronized void play(String resource, double volume) {
        if (!closed && aktif && session.isLoggedIn()) backend.play(resource, volume);
    }
    @Override public synchronized void stop() { backend.stop(); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true; detach.run(); backend.close();
    }
    /** A released view or old identity cannot play an effect from a delayed handler. */
    public View forView() { return new View(); }
    public final class View implements AutoCloseable {
        private final long generation = session.getGeneration();
        private boolean released;
        public void play(Efek effect) {
            synchronized (SoundEffects.this) {
                if (!released && generation == session.getGeneration()) SoundEffects.this.play(effect.path, effect.volume);
            }
        }
        @Override public void close() { synchronized (SoundEffects.this) { released = true; } }
    }
}
