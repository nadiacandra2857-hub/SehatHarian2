package com.sehatharian.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

public class DialogHelper {

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Terjadi Kendala");
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Perhatian Kesehatan");
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void applyDialogStyle(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        try {
            String css = DialogHelper.class.getResource("/css/styles.css").toExternalForm();
            pane.getStylesheets().add(css);
            pane.getStyleClass().add("modern-dialog");
        } catch (Exception ignored) {}
    }
}
