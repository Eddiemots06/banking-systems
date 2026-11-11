package com.bac.banking.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.util.Optional;

/** Small helpers for showing messages consistently across the views. */
final class UiHelper {
    private UiHelper() {
    }

    static void error(Window owner, String message) {
        alert(Alert.AlertType.ERROR, owner, "Something went wrong", message);
    }

    static void info(Window owner, String header, String message) {
        alert(Alert.AlertType.INFORMATION, owner, header, message);
    }

    static boolean confirm(Window owner, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(owner);
        alert.setHeaderText(header);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("heading");
        return label;
    }

    static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-text");
        label.setWrapText(true);
        return label;
    }

    private static void alert(Alert.AlertType type, Window owner, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
