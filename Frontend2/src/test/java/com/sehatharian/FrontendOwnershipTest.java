package com.sehatharian;

import com.sehatharian.context.ApplicationContext;
import com.sehatharian.controller.*;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.util.SceneNavigator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real factory/controllers, no FXML initialization, toolkit, DB or audio devices. */
class FrontendOwnershipTest {
    private FrontendOwnershipFixtures storage;
    private ApplicationContext context;
    @BeforeEach void setup() throws Exception {
        storage=new FrontendOwnershipFixtures();
        context=new ApplicationContext(storage.accounts,new PenggunaRepository(storage.db::connection),
            id->null,new ReminderTestPorts.Scheduler(),Clock.systemUTC(),Runnable::run,new ReminderTestPorts.Output());
        context.authService().login("a@example.test",FrontendOwnershipFixtures.SECRET);
    }
    @AfterEach void close() { context.close(); }
    @ParameterizedTest @CsvSource({
        "DashboardController,handleQuickAddHydration",
        "CatatanKebiasaanController,handleQuick250",
        "JadwalTidurController,handleSimpanTidur",
        "TantanganController,handleMuatUlangTantangan",
        "ProfilPengaturanController,handleSimpanProfil",
        "OnboardingController,handleFinish",
        "EvaluasiMingguanController,handleJalankanEvaluasi",
        "StatistikMingguanController,muatDanTampilkanGrafik",
        "RekomendasiController,muatRekomendasi"
    })
    void oldControllerCannotRunAsReplacementUser(String className,String action) throws Exception {
        var factory=context.navigator().newLoader(SceneNavigator.Page.MAIN.path()).getControllerFactory();
        var type=Class.forName("com.sehatharian.controller."+className);
        Object controller=factory.call(type);
        var before=storage.db.snapshot("pengguna");
        context.authService().login("b@example.test",FrontendOwnershipFixtures.SECRET);
        InvocationTargetException failure=assertThrows(InvocationTargetException.class,()->type.getMethod(action).invoke(controller));
        assertInstanceOf(SecurityException.class,failure.getCause());
        assertEquals(0,storage.db.opens);assertEquals(before,storage.db.snapshot("pengguna"));
        ((AutoCloseable)controller).close();
    }
    @Test void releasedControllerCannotRecoverOnSameIdentity() throws Exception {
        var controller=new ProfilPengaturanController(context.session(),context.penggunaService());
        controller.close();assertThrows(SecurityException.class,controller::handleSimpanProfil);
        assertEquals(0,storage.db.opens);
    }
    @Test void delayedResultCannotBeAppliedToNewOwnersUi() throws Exception {
        var access=new SessionAccess(context.session());
        var result=List.of("Synthetic A data"); List<String> ui=new ArrayList<>();
        Runnable callback=()->access.apply(()->ui.addAll(result));
        context.authService().login("b@example.test",FrontendOwnershipFixtures.SECRET);
        assertThrows(SecurityException.class,callback::run);assertTrue(ui.isEmpty());
    }
}
