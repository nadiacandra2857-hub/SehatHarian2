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
    private SleepScheduleProvider.Schedule current;
    private Instant lastCheck;
    private boolean localOverride;
    // Dispatch expiry only. The local six-second asset determines playback duration.
    private long alarmExpiry;
    public static final int ALARM_DISPATCH_SECONDS = 5;
    private String scheduleStatus() {
        if(current==null)return "Pengingat tidur belum diatur";
        var now=LocalDateTime.now(clock);
        var day=now.toLocalDate().isBefore(current.from())?current.from():now.toLocalDate();
        var next=day.atTime(current.bedtime());
        if(next.isBefore(now))next=next.plusDays(1);
        return (aktif?"Berikutnya · ":"Pengingat nonaktif · jadwal ")+next+" WIB (Asia/Jakarta)";
    }
    private boolean aktif = true, closed;
    private long revision, workerEpoch, preferenceRevision;
    private LocalDateTime notified, alarmed;
    private String status = "Pengingat tidur belum diatur";
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
    public synchronized String nextScheduleText() {
        if(current==null)return "Belum ada jadwal tersimpan yang berhasil dimuat.";
        var now=LocalDateTime.now(clock);
        var day=now.toLocalDate().isBefore(current.from())?current.from():now.toLocalDate();
        var next=day.atTime(current.bedtime());
        if(next.isBefore(now)||next.equals(alarmed))next=next.plusDays(1);
        var wake=current.wake()==null?null:next.toLocalDate().atTime(current.wake());
        if(wake!=null&&!wake.isAfter(next))wake=wake.plusDays(1);
        return (aktif?"Pengingat aktif · ":"Pengingat nonaktif · ")+next+(wake==null?"":" → "+wake)+" WIB (Asia/Jakarta)";
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
        target = null; current=null;lastCheck=null;localOverride=false; aktif = true; notified = null; alarmed = null;
        status = "Pengingat tidur belum diatur";
        if (cancelWorker != null) { cancelWorker.run(); cancelWorker = null; }
        for (var listener : listeners) listener.live = false;
        listeners.clear();
        output.stop();
    }
    /** Clearing first also prevents an empty/failed refresh retaining an older target. */
    public synchronized void reloadSchedule() {
        if (closed) return;
        target = null; current=null;localOverride=false; revision++; workerEpoch++;
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
                    current=schedule;target = schedule == null ? null : schedule.bedtime();
                    if(schedule!=null)output.configureSound(schedule.sound(),schedule.volume());
                    if (schedule != null && preference == preferenceRevision) aktif = schedule.enabled();
                    status = scheduleStatus();
                    {
                        long epoch = workerEpoch;
                        cancelWorker = scheduler.everyFiveSeconds(() -> safeCheck(generation, epoch));
                    }
                    emit(status, false);
                }
            } catch (Exception e) {
                synchronized (this) {
                    if (!validLoad(generation, version)) return;
                    target = null;
                    status = "Jadwal tidur belum dapat dimuat";
                    cancelWorker=scheduler.everyFiveSeconds(()->safeCheck(generation,version));
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
        aktif = enabled;localOverride=true; preferenceRevision++;
        revision++; // invalidate UI already queued when toggling
        output.stop();
    }
    public synchronized boolean isAktif() { return aktif; }
    public synchronized LocalTime getTargetJamTidur() { return target; }
    public synchronized String getStatus() { return status; }
    private synchronized void safeCheck(long generation, long epoch) {
        if (closed || generation != session.getGeneration() || epoch != workerEpoch) return;
        try {
            var saved=schedules.latest(session.getUserId());
            if(!Objects.equals(saved,current)){
                current=saved;target=saved==null?null:saved.bedtime();if(!localOverride)aktif=saved!=null&&saved.enabled();
                revision++;output.stop();status=scheduleStatus();emit(status,false);
            }
            if(saved!=null)output.configureSound(saved.sound(),saved.volume());
            check();
        } catch(Exception failure){target=null;current=null;revision++;output.stop();status="Jadwal tidur belum dapat dimuat";emit(status,false);}
    }
    private synchronized void check() {
        if (closed || !session.isLoggedIn() || !aktif || target == null) return;
        var instant=clock.instant();
        if(lastCheck!=null&&(Duration.between(lastCheck,instant).getSeconds()>90||instant.isBefore(lastCheck))){lastCheck=instant;var jumped=LocalDateTime.ofInstant(instant,ZONE);var event=jumped.toLocalDate().atTime(target);long distance=Duration.between(jumped,event).getSeconds();if(distance < -43200)event=event.plusDays(1);else if(distance > 43200)event=event.minusDays(1);if(Math.abs(Duration.between(jumped,event).toMillis())<=10000)alarmed=event;status="Waktu berubah atau perangkat kembali aktif. Pengingat lama dilewati.";emit(status,false);return;}
        lastCheck=instant;
        var now = LocalDateTime.now(clock);
        var occurrence = now.toLocalDate().atTime(target);
        long seconds = Duration.between(now, occurrence).getSeconds();
        if (seconds < -43200) occurrence = occurrence.plusDays(1);
        else if (seconds > 43200) occurrence = occurrence.minusDays(1);
        if(current!=null&&occurrence.toLocalDate().isBefore(current.from()))return;
        seconds = Duration.between(now, occurrence).getSeconds();
        long millis = Duration.between(now, occurrence).toMillis();
        if (millis >= 570000 && millis <= 630000 && !occurrence.equals(notified)) {
            notified = occurrence;
            emit("Pengingat: sekitar 10 menit lagi waktu tidur " + target + " WIB (Asia/Jakarta).", false);
        }
        if (millis >= -10000 && millis <= 10000 && !occurrence.equals(alarmed)) {
            alarmed = occurrence;
            boolean claimed=false;
            if(output.readyForAlarm())try{claimed=schedules.claim(session.getUserId(),current,occurrence);}catch(Exception failure){/* banner survives claim failure */}
            alarmExpiry=System.currentTimeMillis()+ALARM_DISPATCH_SECONDS*1000;
            emit("Waktu tidur tiba (WIB). Saatnya beristirahat. "+(claimed?"":"Suara belum siap, dimatikan, atau telah ditangani tab lain."),claimed);
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
                try { output.playUntil("/sounds/sleep_gentle.wav", .35,alarmExpiry); }
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
        if (!closed) {alarmExpiry=System.currentTimeMillis()+10000;emit("Uji pengingat visual (WIB). Periksa suara pada tab/perangkat ini.",true);}
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        resetState();
        detachIdentity.run();
        scheduler.close();
    }
}
