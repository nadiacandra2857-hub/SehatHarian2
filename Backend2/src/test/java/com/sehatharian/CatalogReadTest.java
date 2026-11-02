package com.sehatharian;

import com.sehatharian.repository.RekomendasiRepository;
import com.sehatharian.repository.TantanganRepository;
import com.sehatharian.repository.PenggunaRepository.ConnectionFactory;
import com.sehatharian.service.DemoCatalog;
import com.sehatharian.service.RekomendasiService;
import com.sehatharian.storage.ReadResult;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogReadTest {
    @Test void connectionFailureNeverReturnsSamples() {
        ConnectionFactory offline = () -> { throw new SQLException("Sensitive server detail"); };
        var recommendations = new RekomendasiRepository(offline);
        var challenges = new TantanganRepository(offline);
        assertThrows(SQLException.class, recommendations::getAll);
        assertThrows(SQLException.class, () -> recommendations.getFiltered("menu", "semua", "semua", 10));
        assertThrows(SQLException.class, challenges::getAll);
        assertThrows(SQLException.class, () -> challenges.getByFilter("semua", "semua", 10));
        assertThrows(SQLException.class, () -> challenges.getById(1));
        var result = new RekomendasiService(recommendations).bacaKatalog("menu", "semua", "semua");
        assertEquals(ReadResult.Source.UNAVAILABLE, result.source());
        assertTrue(result.value().isEmpty());
        assertFalse(result.message().contains("Sensitive"));
    }

    @Test void queryFailureAndCloseFailureDoNotReturnPartialRowsOrSamples() {
        var db = new OwnedJdbc();
        db.failRead = true;
        assertThrows(SQLException.class, new RekomendasiRepository(db::connection)::getAll);
        assertThrows(SQLException.class, new TantanganRepository(db::connection)::getAll);
        db.failRead = false;
        db.failConnectionClose = true;
        assertEquals(ReadResult.Source.UNAVAILABLE,
            new RekomendasiService(new RekomendasiRepository(db::connection)).bacaKatalog(null,null,null).source());
    }

    @Test void emptyDatabaseAndMissingChallengeRemainEmpty() throws Exception {
        var db = new OwnedJdbc();
        var result = new RekomendasiService(new RekomendasiRepository(db::connection))
            .bacaKatalog("semua", "semua", "semua");
        assertEquals(ReadResult.Source.DATABASE, result.source());
        assertEquals(List.of(), result.value().orElseThrow());
        assertNotNull(result.observedAt());
        var challenges = new TantanganRepository(db::connection);
        assertNull(challenges.getById(1)); // ID 1 exists in legacy samples, not this SQL fixture.
        assertTrue(challenges.getAll().isEmpty());
        assertEquals(db.opens, db.closedConnections);
    }

    @Test void databaseRowsKeepTheirOwnIdentityAndDemoCannotModifyThem() throws Exception {
        var db = new OwnedJdbc();
        db.add("rekomendasi", "id", 87, "judul", "SQL only", "tipe", "aktivitas");
        var result = new RekomendasiService(new RekomendasiRepository(db::connection)).bacaKatalog(null,null,null);
        assertEquals(ReadResult.Source.DATABASE, result.source());
        assertEquals(87, result.value().orElseThrow().getFirst().getId());
        var demo = DemoCatalog.recommendations(null,null,null);
        assertEquals(ReadResult.Source.DEMO, demo.source());
        demo.value().orElseThrow().getFirst().setJudul("Edited sample");
        assertNotEquals("Edited sample", DemoCatalog.recommendations(null,null,null).value().orElseThrow().getFirst().getJudul());
        assertEquals("SQL only", result.value().orElseThrow().getFirst().getJudul());
        assertEquals(1, db.opens); // preview opens no connection and executes no writes
        assertEquals(0, db.executions);
        assertThrows(UnsupportedOperationException.class, () -> demo.value().orElseThrow().clear());
    }

    @Test void demoIsExplicitAndRespectsFiltersIncludingEmptyResults() {
        var result = DemoCatalog.recommendations("menu", "luar_rumah", "lansia");
        assertFalse(result.value().orElseThrow().isEmpty());
        assertTrue(result.value().orElseThrow().stream().allMatch(r -> "menu".equals(r.getTipe())
            && ("semua".equals(r.getGolonganUsia()) || "lansia".equals(r.getGolonganUsia()))
            && ("semua".equals(r.getGolonganKesibukan()) || "luar_rumah".equals(r.getGolonganKesibukan()))));
        var empty = DemoCatalog.recommendations("unknown-type", null, null);
        assertEquals(ReadResult.Source.DEMO, empty.source());
        assertTrue(empty.value().orElseThrow().isEmpty());
    }

    @Test void readResultNeverConvertsAuthorizationFailureToUnavailable() {
        assertThrows(SecurityException.class,
            () -> ReadResult.database(() -> { throw new SecurityException("Stale scope"); }));
    }
}
