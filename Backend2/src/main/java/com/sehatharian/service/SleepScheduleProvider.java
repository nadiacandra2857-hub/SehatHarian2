package com.sehatharian.service;

import java.time.LocalTime;

/** Null means no committed SQL schedule; failures must propagate, never use demo data. */
@FunctionalInterface
public interface SleepScheduleProvider {
    record Schedule(LocalTime bedtime, boolean enabled) {
        public Schedule { java.util.Objects.requireNonNull(bedtime); }
    }
    Schedule latest(int userId) throws Exception;
}
