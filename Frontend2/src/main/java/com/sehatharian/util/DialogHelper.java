package com.sehatharian.util;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;

/** Dialogs belong to the originating view/window, including independent JPro instances. */
public final class DialogHelper {
    private DialogHelper() {}

    public static void showSuccess(Node owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, null, message);
    }
    public static void showInfo(Node owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, null, message);
    }
    public static void showError(Node owner, String title, String message) {
        show(owner, Alert.AlertType.ERROR, title, "Terjadi Kendala", message);
    }
    public static void showWarning(Node owner, String title, String message) {
        show(owner, Alert.AlertType.WARNING, title, "Periksa kembali", message);
    }
    public static boolean showConfirmation(Node owner, String title, String message) {
        return show(owner, Alert.AlertType.CONFIRMATION, title, null, message)
                .filter(ButtonType.OK::equals).isPresent();
    }
    private static java.util.Optional<ButtonType> show(Node owner, Alert.AlertType type,
            String title, String header, String message) {
        if (owner == null || owner.getScene() == null || owner.getScene().getWindow() == null)
            throw new IllegalStateException("Dialog memerlukan halaman aktif.");
        Window window = owner.getScene().getWindow();
        Alert alert = new Alert(type);
        alert.initOwner(window);
        alert.setTitle(title);
        alert.setHeaderText(header);
        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(DialogHelper.class.getResource("/css/styles.css").toExternalForm());
        pane.getStylesheets().add(DialogHelper.class.getResource("/css/ui2.css").toExternalForm());
        pane.getStyleClass().addAll("ui2-dialog", "modern-dialog");
        pane.setMinWidth(0);
        pane.setPrefWidth(Math.max(240, Math.min(440, window.getWidth() - 24)));
        Label text = new Label(message == null ? "" : message);
        text.setWrapText(true);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(text);
        scroll.setFitToWidth(true);
        scroll.setMinWidth(0);
        scroll.setPrefViewportWidth(Math.max(160, Math.min(360, window.getWidth() - 88)));
        double estimatedHeight = Math.max(64, (text.getText().length() / 38.0 + text.getText().lines().count()) * 18);
        scroll.setPrefViewportHeight(Math.min(estimatedHeight, Math.max(100, Math.min(360, window.getHeight() - 200))));
        scroll.getStyleClass().add("ui2");
        pane.setContent(scroll);
        if (type == Alert.AlertType.CONFIRMATION) {
            ((Button)pane.lookupButton(ButtonType.OK)).setText("Ya");
            ((Button)pane.lookupButton(ButtonType.CANCEL)).setText("Batal");
        }
        // A view replaced during a nested dialog must not leave a modal window blocking its successor.
        javafx.beans.value.ChangeListener<javafx.scene.Scene> detached = (o, before, after) -> {
            if (after != window.getScene()) alert.close();
        };
        owner.sceneProperty().addListener(detached);
        try { return alert.showAndWait(); }
        finally { owner.sceneProperty().removeListener(detached); }
    }
}
