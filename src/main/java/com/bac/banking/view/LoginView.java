package com.bac.banking.view;

import com.bac.banking.controller.LoginController;
import com.bac.banking.controller.Session;
import com.bac.banking.exception.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Boundary class for the Login use case. Collects the username and password and passes them
 * to the LoginController; contains no authentication logic itself.
 */
public class LoginView {
    private final LoginController controller;
    private final ViewNavigator navigator;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = UiHelper.errorLabel();

    public LoginView(LoginController controller, ViewNavigator navigator) {
        this.controller = controller;
        this.navigator = navigator;
    }

    public Parent build() {
        Label title = UiHelper.heading("BAC Bank");
        Label subtitle = new Label("Sign in to Internet Banking");
        subtitle.getStyleClass().add("subtitle");

        usernameField.setPromptText("Username");
        passwordField.setPromptText("Password");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.add(new Label("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);

        Button loginButton = new Button("Log in");
        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().add("primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin());

        Hyperlink register = new Hyperlink("New customer? Register here");
        register.setOnAction(e -> navigator.showRegister());

        VBox card = new VBox(14, title, subtitle, form, errorLabel, loginButton, register);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(30));
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("app-background");
        return root;
    }

    private void handleLogin() {
        try {
            Session.Role role = controller.login(usernameField.getText(), passwordField.getText());
            if (role == Session.Role.ADMINISTRATOR) {
                navigator.showAdminDashboard();
            } else {
                navigator.showCustomerDashboard();
            }
        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
            passwordField.clear();
        }
    }
}
