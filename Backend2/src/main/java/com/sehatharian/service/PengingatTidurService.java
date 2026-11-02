package com.sehatharian.service;

import com.sehatharian.model.UserSession;
import com.sehatharian.util.AudioOutput;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/** Session reminder: reset is reusable; close is terminal. No JavaFX/toolkit dependency. */
public final class PengingatTidurService implements AutoCloseable {
    public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private final UserSession session;
    private final SleepScheduleProvider schedules;
    private final ReminderScheduler scheduler;
    private final Clock clock;
    private final Consumer<Runnable> ui;
    private final AudioOutput output;
    private final Runnable detachIdentity;
    private final List<Subscription> listeners = new ArrayList<>();
    private Runnable cancelWorker;
    private LocalTime target;
    private boolean aktif = true, closed;
    private long revision, workerEpoch, preferenceRevision;
    private LocalDateTime notified, alarmed;
    private String status = "Belum ada jadwal SQL aktif (WIB).";
    private final class Subscription {
        final long generation = session.getGeneration();
        final Consumer<String> listener;
        boolean live = true;
        Subscription(Consumer<String> listener) { this.listener = listener; }
    }

    public PengingatTidurService(UserSession session, SleepScheduleProvider schedules,
            ReminderScheduler scheduler, Clock clock, Consumer<Runnable> ui, AudioOutput output) {
        this.session = Objects.requireNonNull(session);
        this.schedules = Objects.requireNonNull(schedules);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.clock = Objects.requireNonNull(clock).withZone(ZONE);
        this.ui = Objects.requireNonNull(ui);
        this.output = Objects.requireNonNull(output);
        detachIdentity = session.onIdentityChanged(() -> {
            reset();
            if (session.isLoggedIn()) reloadSchedule();
        });
    }
    private boolean valid(long generation, long version) {
        return !closed && session.isLoggedIn() && generation == session.getGeneration() && version == revision;
    }
    public synchronized Runnable subscribe(Consumer<String> listener) {
        if (closed) throw new IllegalStateException("Pengingat sudah ditutup.");
        session.requireActiveUser();
        var subscription = new Subscription(Objects.requireNonNull(listener));
        listeners.add(subscription);
        return () -> { synchronized (this) {
            if (!subscription.live) return;
            subscription.live = false;
            listeners.remove(subscription);
            if (listeners.isEmpty()) output.stop();
        }};
    }
    public synchronized void reset() {
        if (!closed) resetState();
    }
    private void resetState() {
        revision++; workerEpoch++;
        target = null; aktif = true; notified = null; alarmed = null;
        status = "Belum ada jadwal SQL aktif (WIB).";
        if (cancelWorker != null) { cancelWorker.run(); cancelWorker = null; }
        for (var listener : listeners) listener.live = false;
        listeners.clear();
        output.stop();
    }
    /** Clearing first also prevents an empty/failed refresh retaining an older target. */
    public synchronized void reloadSchedule() {
        if (closed) return;
        target = null; revision++; workerEpoch++;
        output.stop();
        if (cancelWorker != null) { cancelWorker.run(); cancelWorker = null; }
        if (!session.isLoggedIn()) return;
        long generation = session.getGeneration(), version = workerEpoch, preference = preferenceRevision;
        int userId = session.getUserId();
        status = "Memuat jadwal SQL (WIB).";
        scheduler.execute(() -> {
            try {
                synchronized (this) { if (!validLoad(generation, version)) return; }
                var schedule = schedules.latest(userId);
                synchronized (this) {
                    if (!validLoad(generation, version)) return;
                    target = schedule == null ? null : schedule.bedtime();
                    if (schedule != null && preference == preferenceRevision) aktif = schedule.enabled();
                    status = schedule == null ? "Belum ada jadwal SQL aktif (WIB)." : "Jadwal SQL " + target + " WIB (Asia/Jakarta)";
                    if (target != null) {
                        long epoch = workerEpoch;
                        cancelWorker = scheduler.everyFiveSeconds(() -> safeCheck(generation, epoch));
                    }
                    emit(status, false);
                }
            } catch (Exception e) {
                synchronized (this) {
                    if (!validLoad(generation, version)) return;
                    target = null;
                    status = "Jadwal SQL tidak tersedia; pengingat belum dijalankan (WIB).";
                    emit(status, false);
                }
                System.err.println("Pembacaan jadwal pengingat gagal; tidak menggunakan data demo.");
            }
        });
    }
    private boolean validLoad(long generation, long epoch) {
        return !closed && session.isLoggedIn() && generation == session.getGeneration() && epoch == workerEpoch;
    }
    public synchronized void setAktif(boolean enabled) {
        if (closed || !session.isLoggedIn()) return;
        aktif = enabled; preferenceRevision++;
        revision++; // invalidate UI already queued when toggling
        output.stop();
    }
    public synchronized boolean isAktif() { return aktif; }
    public synchronized LocalTime getTargetJamTidur() { return target; }
    public synchronized String getStatus() { return status; }
    private synchronized void safeCheck(long generation, long epoch) {
        if (closed || generation != session.getGeneration() || epoch != workerEpoch) return;
        try { check(); }
        catch (RuntimeException e) { System.err.println("Pemeriksaan pengingat gagal; pemeriksaan berikutnya tetap aktif."); }
    }
    private synchronized void check() {
        if (closed || !session.isLoggedIn() || !aktif || target == null) return;
        var now = LocalDateTime.now(clock);
        var occurrence = now.toLocalDate().atTime(target);
        long seconds = Duration.between(now, occurrence).getSeconds();
        if (seconds < -43200) occurrence = occurrence.plusDays(1);
        else if (seconds > 43200) occurrence = occurrence.minusDays(1);
        seconds = Duration.between(now, occurrence).getSeconds();
        if (seconds >= 570 && seconds <= 630 && !occurrence.equals(notified)) {
            notified = occurrence;
            emit("Pengingat: sekitar 10 menit lagi waktu tidur " + target + " WIB (Asia/Jakarta).", false);
        }
        if (seconds >= -10 && seconds <= 10 && !occurrence.equals(alarmed)) {
            alarmed = occurrence;
            emit("Waktu tidur tiba (WIB). Saatnya beristirahat; ambang proyek minimal 5 jam.", true);
        }
    }
    private synchronized void emit(String message, boolean alarm) {
        long generation = session.getGeneration(), version = revision;
        if (!valid(generation, version)) return;
        var recipients = listeners.stream().filter(s -> s.live && s.generation == generation).toList();
        if (recipients.isEmpty()) return;
        try { ui.accept(() -> { synchronized (this) {
            if (!valid(generation, version)) return;
            var live = recipients.stream().filter(s -> s.live && s.generation == generation).toList();
            if (live.isEmpty()) return;
            if (alarm) {
                try { output.play("/sounds/bell_reminder.wav", 1.0); }
                catch (RuntimeException | LinkageError e) { System.err.println("Audio pengingat tidak tersedia; banner tetap ditampilkan."); }
            }
            for (var recipient : live) {
                if (!valid(generation, version)) break;
                if (!recipient.live) continue;
                try { recipient.listener.accept(message); }
                catch (RuntimeException e) { System.err.println("Tampilan pengingat tidak tersedia."); }
            }
        }}); } catch (RuntimeException e) {
            System.err.println("Antrean UI pengingat tidak tersedia; tidak menjalankan UI pada worker.");
        }
    }
    public synchronized void ujiCobaPengingat() {
        if (!closed) emit("Uji pengingat visual (WIB). Suara hanya tersedia pada backend audio yang didukung.", true);
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        resetState();
        detachIdentity.run();
        scheduler.close();
    }
}
