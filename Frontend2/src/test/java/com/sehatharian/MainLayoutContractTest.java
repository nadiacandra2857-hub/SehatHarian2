package com.sehatharian;

import com.sehatharian.controller.MainLayoutController;
import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.jupiter.api.Assertions.*;

/** XML/binding checks only: no rendering, FXMLLoader.load, device, database or toolkit. */
class MainLayoutContractTest {
    private Document layout() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var input = getClass().getResourceAsStream("/fxml/MainLayout.fxml")) {
            assertNotNull(input); return factory.newDocumentBuilder().parse(input);
        }
    }
    private Element id(Document doc, String id) {
        var all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (id.equals(element.getAttributeNS("http://javafx.com/fxml/1", "id"))) return element;
        }
        fail("Missing control: " + id); return null;
    }
    @Test void accountActionsHaveLabelsTouchTargetsAndCorrectBindings() throws Exception {
        var doc = layout();
        for (String name : new String[]{"btnNavProfil", "btnLogout"}) {
            Element button = id(doc, name);
            assertFalse(button.getAttribute("text").isBlank());
            assertFalse(button.getAttribute("accessibleText").isBlank());
            assertTrue(Double.parseDouble(button.getAttribute("minHeight")) >= 44);
            assertSame(id(doc, "sidebarUserCard"), button.getParentNode());
            assertNotEquals("false", button.getAttribute("visible"));
            assertNotEquals("false", button.getAttribute("managed"));
        }
        assertEquals("#handleLogout", id(doc, "btnLogout").getAttribute("onAction"));
        assertEquals("#handleNavProfil", id(doc, "btnNavProfil").getAttribute("onAction"));
    }
    @Test void sidebarAllowsVerticalReachWithoutHorizontalScrollAndAllActionsResolve() throws Exception {
        var doc = layout();
        assertEquals("true", id(doc, "sidebarScroll").getAttribute("fitToWidth"));
        assertEquals("NEVER", id(doc, "sidebarScroll").getAttribute("hbarPolicy"));
        var all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            String action = ((Element)all.item(i)).getAttribute("onAction");
            if (action.startsWith("#")) assertNotNull(MainLayoutController.class.getDeclaredMethod(action.substring(1)));
        }
    }
}
