package com.sehatharian;

import com.sehatharian.controller.RekomendasiController;
import com.sehatharian.model.Rekomendasi;
import com.sehatharian.repository.RekomendasiRepository;
import com.sehatharian.service.RekomendasiService;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Controller routing without a toolkit; rendering remains a separate acceptance check. */
class CatalogPreviewTest {
    @Test void previewRequiresExplicitSelectionAndIsLocalToOneView() throws Exception {
        var fixture = new FrontendOwnershipFixtures();
        int[] reads = {0};
        var service = new RekomendasiService(new RekomendasiRepository() {
            @Override public List<Rekomendasi> getFiltered(String type, String context, String age, int minutes) throws SQLException {
                reads[0]++;
                throw new SQLException("Offline fixture");
            }
        });
        var first = new RekomendasiController(fixture.session, service);
        first.muatRekomendasi();
        assertEquals(1, reads[0]);
        first.handlePreviewDemo();
        first.handleResetFilter();
        assertEquals(1, reads[0]); // failure did not activate demo; explicit preview avoids SQL
        var second = new RekomendasiController(fixture.session, service);
        second.muatRekomendasi();
        assertEquals(2, reads[0]); // new view starts with SQL even while another previews examples
        first.handleLoadDatabase();
        assertEquals(3, reads[0]);
        first.handleFilterChanged();
        assertEquals(4, reads[0]); // failed reload stays in database mode
        first.close();
        assertThrows(SecurityException.class, first::handlePreviewDemo);
        second.close();
    }

    @Test void changingIdentityRejectsPreviewAndDatabaseHandlersOfOldView() throws Exception {
        var fixture = new FrontendOwnershipFixtures();
        var controller = new RekomendasiController(fixture.session, fixture.rekomendasi());
        controller.handlePreviewDemo();
        fixture.loginB();
        assertThrows(SecurityException.class, controller::handlePreviewDemo);
        assertThrows(SecurityException.class, controller::handleLoadDatabase);
        assertThrows(SecurityException.class, controller::handleFilterChanged);
        controller.close();
    }
}
