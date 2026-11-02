package com.sehatharian.repository;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.config.DatabaseConfig;
import com.sehatharian.service.SleepScheduleProvider;

/** Read-only reminder projection, intentionally bypasses the legacy demo fallback repository. */
public final class SqlSleepScheduleProvider implements SleepScheduleProvider {
    private final com.sehatharian.model.UserSession session;
    private final PenggunaRepository.ConnectionFactory connections;
    public SqlSleepScheduleProvider(com.sehatharian.model.UserSession session) { this(session, DatabaseConfig::getConnection); }
    public SqlSleepScheduleProvider(com.sehatharian.model.UserSession session, PenggunaRepository.ConnectionFactory connections) {
        this.session = java.util.Objects.requireNonNull(session);
        this.connections = java.util.Objects.requireNonNull(connections);
    }
    @Override public Schedule latest(int userId) throws java.sql.SQLException {
        var access = new SessionAccess(session);
        access.owner(userId);
        try (var connection = OwnedRepository.open(access, connections);
             var statement = connection.prepareStatement(
                 "SELECT jam_tidur, pengingat_aktif FROM jadwal_tidur WHERE pengguna_id = ? ORDER BY tanggal DESC, id DESC LIMIT 1")) {
            statement.setInt(1, userId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return null;
                var time = result.getTime("jam_tidur");
                if (time == null) throw new java.sql.SQLException("Jadwal tidak valid.");
                return access.result(new Schedule(time.toLocalTime(), result.getBoolean("pengingat_aktif")));
            }
        }
    }
}
