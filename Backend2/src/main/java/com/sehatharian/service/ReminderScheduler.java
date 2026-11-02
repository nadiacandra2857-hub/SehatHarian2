package com.sehatharian.service;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Small scheduling port: deterministic queue in tests, one daemon executor in production. */
public interface ReminderScheduler extends AutoCloseable {
    void execute(Runnable work);
    Runnable everyFiveSeconds(Runnable work);
    @Override void close();

    static ReminderScheduler worker() {
        return new ReminderScheduler() {
            private final java.util.concurrent.ScheduledExecutorService executor =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread thread = new Thread(r, "PengingatTidur-Instance");
                    thread.setDaemon(true);
                    return thread;
                });
            public void execute(Runnable work) { executor.execute(work); }
            public Runnable everyFiveSeconds(Runnable work) {
                var task = executor.scheduleAtFixedRate(work, 5, 5, TimeUnit.SECONDS);
                return () -> task.cancel(false);
            }
            public void close() { executor.shutdownNow(); }
        };
    }
}
