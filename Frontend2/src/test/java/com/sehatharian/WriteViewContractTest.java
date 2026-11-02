package com.sehatharian;

import com.sehatharian.controller.CreateAccountController;
import com.sehatharian.util.SceneNavigator;
import org.junit.jupiter.api.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** FXML/factory wiring only; no rendering/toolkit or database. */
class WriteViewContractTest {
    @Test void uncertainWriteButtonsKeepTheirSemanticTagAfterMarkupChanges() throws Exception {
        var methods=Set.of("handleQuick250","handleQuick500","handleQuick600","handleTambahHidrasiManual","handleSimpanAktivitas","handleSimpanMakan","handleSimpanTidur","handleJalankanEvaluasi","handleSimpanProfil","handleFinish","handleMuatUlangTantangan");
        Set<String> found=new HashSet<>();
        for(var page:SceneNavigator.Page.values()) {
            var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            try(var input=getClass().getResourceAsStream(page.path())) {
                var doc=factory.newDocumentBuilder().parse(input);var buttons=doc.getElementsByTagName("Button");
                for(int i=0;i<buttons.getLength();i++) {
                    Element button=(Element)buttons.item(i);String action=button.getAttribute("onAction").replace("#","");
                    if(methods.contains(action)) { found.add(action);assertTrue(button.getAttribute("styleClass").contains("write-command"),action); }
                }
            }
        }
        assertEquals(methods,found);
    }
    @Test void publicRegistrationUsesItsOwnContextSessionAndHasTerminalViewCleanup() throws Exception {
        try(var context=FrontendAuthFixture.context()) {
            Object controller=context.navigator().newLoader(SceneNavigator.Page.REGISTER.path()).getControllerFactory().call(CreateAccountController.class);
            var field=CreateAccountController.class.getDeclaredField("session");field.setAccessible(true);
            assertSame(context.session(),field.get(controller));
            ((AutoCloseable)controller).close();
            ((CreateAccountController)controller).handleRegister(); // must return before touching absent FXML controls
            assertFalse(context.session().isLoggedIn());
        }
    }
}
