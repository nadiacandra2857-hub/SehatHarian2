package com.sehatharian;

import com.sehatharian.service.ReminderScheduler;
import com.sehatharian.util.AudioOutput;
import java.time.*;
import java.util.*;

/** No devices, toolkit, threads, network or wall-clock waits. */
final class ReminderTestPorts {
    static final class Output implements AudioOutput {
        int plays, stops, closes;
        boolean failPlay;
        public void play(String resource, double volume) {
            if (failPlay) throw new IllegalStateException("Synthetic output failure");
            plays++;
        }
        public void stop() { stops++; }
        public void close() { closes++; stops++; }
    }
    static final class Scheduler implements ReminderScheduler {
        final Deque<Runnable> loads = new ArrayDeque<>();
        final List<Task> tasks = new ArrayList<>();
        int closes;
        static final class Task {
            final Runnable work; boolean cancelled;
            Task(Runnable work) { this.work = work; }
        }
        public void execute(Runnable work) { loads.add(work); }
        public Runnable everyFiveSeconds(Runnable work) {
            Task task = new Task(work); tasks.add(task); return () -> task.cancelled = true;
        }
        void load() { while (!loads.isEmpty()) loads.remove().run(); }
        void tick() { for (Task task : List.copyOf(tasks)) if (!task.cancelled) task.work.run(); }
        long liveTasks() { return tasks.stream().filter(t -> !t.cancelled).count(); }
        public void close() { closes++; for (Task task : tasks) task.cancelled = true; loads.clear(); }
    }
    static final class Time extends Clock {
        Instant instant = Instant.parse("2026-10-31T16:59:55Z");
        boolean failNext;
        public ZoneId getZone() { return ZoneId.of("Asia/Jakarta"); }
        public Clock withZone(ZoneId zone) {
            if (!zone.equals(getZone())) throw new IllegalArgumentException("Unexpected test zone");
            return this;
        }
        public Instant instant() {
            if (failNext) { failNext = false; throw new IllegalStateException("Synthetic clock failure"); }
            return instant;
        }
        void at(String value) { instant = Instant.parse(value); }
    }
}
