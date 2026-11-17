package com.sehatharian.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;

/** Keeps the focused form field in view when the viewport shrinks, including browser keyboard layouts. */
public final class FocusReveal implements AutoCloseable {
    private final ScrollPane scroll;
    private Scene scene;
    private boolean closed;
    private final ChangeListener<Node> focus=(o,a,b)->later();
    private final ChangeListener<Scene> scenes=(o,a,b)->watch(b);
    private final ChangeListener<javafx.geometry.Bounds> viewport=(o,a,b)->later();
    public FocusReveal(ScrollPane scroll) {
        this.scroll=scroll;scroll.sceneProperty().addListener(scenes);scroll.viewportBoundsProperty().addListener(viewport);watch(scroll.getScene());
    }
    private void watch(Scene next) {
        if(scene!=null)scene.focusOwnerProperty().removeListener(focus);
        scene=next;if(scene!=null&&!closed)scene.focusOwnerProperty().addListener(focus);
    }
    private void later(){if(!closed)Platform.runLater(()->{if(!closed&&scene!=null)reveal(scene.getFocusOwner());});}
    public void reveal(Node node) {
        Node content=scroll.getContent();if(content==null||node==null||!node.isVisible())return;
        Node ancestor=node;while(ancestor!=null&&ancestor!=content)ancestor=ancestor.getParent();if(ancestor==null)return;
        var bounds=content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));double h=scroll.getViewportBounds().getHeight();
        double range=Math.max(0,content.getLayoutBounds().getHeight()-h);if(range==0)return;
        double offset=scroll.getVvalue()*range,next=offset;
        if(bounds.getMinY()<offset+12)next=bounds.getMinY()-12;
        else if(bounds.getMaxY()>offset+h-12)next=bounds.getMaxY()-h+12;
        scroll.setVvalue(Math.max(0,Math.min(1,next/range)));
    }
    @Override public void close(){closed=true;watch(null);scroll.sceneProperty().removeListener(scenes);scroll.viewportBoundsProperty().removeListener(viewport);}
}
