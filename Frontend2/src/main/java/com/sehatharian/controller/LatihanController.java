package com.sehatharian.controller;

import com.sehatharian.catalog.Exercise;
import com.sehatharian.model.UserSession;
import com.sehatharian.util.TrainingDialog;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;

/** Private page hosts the same catalog and session engine used by the dialog. */
public final class LatihanController implements AutoCloseable {
    private final UserSession session;
    @FXML private ScrollPane latihanRoot;
    private TrainingDialog training;
    private javafx.scene.Scene focusScene;
    private final javafx.beans.value.ChangeListener<javafx.scene.Node> focusListener=(o,a,b)->
        javafx.application.Platform.runLater(()->revealFocus(b));
    private final javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener=(o,a,b)->watchFocus(b);

    private void watchFocus(javafx.scene.Scene scene) {
        if(focusScene!=null)focusScene.focusOwnerProperty().removeListener(focusListener);
        focusScene=scene;
        if(scene!=null)scene.focusOwnerProperty().addListener(focusListener);
    }

    private void revealFocus(javafx.scene.Node node) {
        if(training==null || node==null || node.getScene()!=focusScene)return;
        javafx.scene.Node parent=node;
        while(parent!=null && parent!=latihanRoot.getContent())parent=parent.getParent();
        if(parent==null)return;
        var content=latihanRoot.getContent();
        var bounds=content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        double viewport=latihanRoot.getViewportBounds().getHeight();
        double range=Math.max(0,content.getLayoutBounds().getHeight()-viewport);
        if(range==0)return;
        double offset=latihanRoot.getVvalue()*range,target=offset;
        if(bounds.getMinY()<offset)target=bounds.getMinY()-12;
        else if(bounds.getMaxY()>offset+viewport)target=bounds.getMaxY()-viewport+12;
        latihanRoot.setVvalue(Math.max(0,Math.min(1,target/range)));
    }

    public LatihanController(UserSession session) { this.session = session; }

    @FXML public void initialize() { latihanRoot.sceneProperty().addListener(sceneListener);watchFocus(latihanRoot.getScene());open(); }

    public void open() {
        if (training != null) return;
        watchFocus(latihanRoot.getScene());
        training = new TrainingDialog(latihanRoot, session, true);
        training.content().setMaxWidth(1060);
        StackPane center = new StackPane(training.content());
        center.setMinWidth(0);
        StackPane.setAlignment(training.content(), Pos.TOP_CENTER);
        latihanRoot.setContent(center);
    }

    public void setManualSelection(Consumer<Exercise> action) { training.setManualSelection(action); }

    public void requestLeave(Runnable leave) {
        training.requestLeave(() -> { close(); leave.run(); });
    }

    @Override public void close() {
        if (training != null) { training.close(); training = null; }
        watchFocus(null);
    }
}
