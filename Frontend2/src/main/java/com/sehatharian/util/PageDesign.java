package com.sehatharian.util;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** View-local accessibility and responsive composition; no session or data state. */
public final class PageDesign {
    private PageDesign() {}
    public static void install(Parent root) {
        if(!root.getStyleClass().contains("ui2"))return;
        if(root instanceof Region r && !root.getProperties().containsKey("ui2-installed")) {
            root.getProperties().put("ui2-installed",true);
            r.widthProperty().addListener((o,a,b)->root.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("compact"),b.doubleValue()<720));
        }
        visit(root);
    }
    private static void visit(Node node) {
        // A wrapped table owns its desktop and mobile presentations; never wrap its internal table again.
        if(node instanceof ResponsiveTable<?>)return;
        if(node instanceof Region r)r.setMinWidth(0);
        if(node instanceof Labeled l) {
            l.setWrapText(true);l.setMinHeight(Region.USE_PREF_SIZE);
            if(l instanceof ButtonBase) { l.setMinHeight(44);l.setMinWidth(44);l.setAccessibleText(l.getText()); }
        }
        if(node instanceof TextInputControl t && t.getAccessibleText()==null)t.setAccessibleText(t.getPromptText());
        if(node instanceof VBox box) {
            var children=java.util.List.copyOf(box.getChildren());
            for(Node child:children) {
                if(child instanceof TableView<?> table) {
                    int index=box.getChildren().indexOf(table);box.getChildren().remove(table);
                    box.getChildren().add(index,new ResponsiveTable<>(table));
                }
            }
            // Visible field captions also provide programmatic labels.
            Label previous=null;
            for(Node child:box.getChildren()) {
                if(child instanceof Label label)previous=label;
                else if(child instanceof Control control && previous!=null) {
                    previous.setLabelFor(control);control.setAccessibleText(previous.getText());previous=null;
                }
            }
        }
        if(node instanceof ScrollPane sp)visit(sp.getContent());
        else if(node instanceof TabPane tabs) {
            if(!tabs.getProperties().containsKey("ui2-tabs")) {
                tabs.getProperties().put("ui2-tabs",true);
                for(Tab tab:tabs.getTabs())if(tab.getGraphic()!=null)tab.getProperties().put("ui2-graphic",tab.getGraphic());
                tabs.widthProperty().addListener((o,a,b)->{for(Tab tab:tabs.getTabs())tab.setGraphic(b.doubleValue()<400?null:(Node)tab.getProperties().get("ui2-graphic"));});
            }
            for(Tab tab:tabs.getTabs())visit(tab.getContent());
        }
        else if(node instanceof Pane p)for(Node child:p.getChildren())visit(child);
    }
}
