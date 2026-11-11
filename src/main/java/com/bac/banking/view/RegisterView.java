package com.bac.banking.view;

import com.bac.banking.controller.CustomerController;
import com.bac.banking.exception.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Boundary class for the Register Customer use case. Collects the form fields and hands them
 * to the CustomerController, which does all validation.
 */
public class RegisterView {
    private final CustomerController controller;
    private final ViewNavigator navigator;

    private final TextField firstName = new TextField();
    private final TextField surname = new TextField();
    private final TextField nationalId = new TextField();
    private final TextField address = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final CheckBox employed = new CheckBox("I am currently employed (needed for a Cheque Account)");
    private final TextField companyName = new TextField();
    private final TextField companyAddress = new TextField();
    private final TextField jobTitle = new TextField();
    private final Label errorLabel = UiHelper.errorLabel();

    public RegisterView(CustomerController controller, ViewNavigator navigator) {
        this.controller = controller;
        this.navigator = navigator;
    }

    public Parent build() {
        nationalId.setPromptText("9 digits");
        phone.setPromptText("e.g. 71234567");
        password.setPromptText("At least 8 characters, letters and numbers");

        GridPane personal = grid();
        int row = 0;
        row = addRow(personal, row, "First name *", firstName);
        row = addRow(personal, row, "Surname *", surname);
        row = addRow(personal, row, "National ID (Omang) *", nationalId);
        row = addRow(personal, row, "Physical address *", address);
        row = addRow(personal, row, "Phone", phone);
        addRow(personal, row, "Email", email);

        GridPane login = grid();
        row = 0;
        row = addRow(login, row, "Username *", username);
        row = addRow(login, row, "Password *", password);
        addRow(login, row, "Confirm password *", confirm);

        GridPane work = grid();
        row = 0;
        row = addRow(work, row, "Company name", companyName);
        row = addRow(work, row, "Company address", companyAddress);
        addRow(work, row, "Job title", jobTitle);
        work.disableProperty().bind(employed.selectedProperty().not());

        Button register = new Button("Create my profile");
        register.getStyleClass().add("primary");
        register.setDefaultButton(true);
        register.setOnAction(e -> handleRegister());

        Button back = new Button("Back to login");
        back.setOnAction(e -> navigator.showLogin());

        HBox buttons = new HBox(10, register, back);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12,
                UiHelper.heading("Register as a new customer"),
                section("Personal details"), personal,
                section("Login details"), login,
                section("Employment"), employed, work,
                errorLabel, buttons);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));
        card.setMaxWidth(620);

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(20));
        wrapper.getStyleClass().add("app-background");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void handleRegister() {
        try {
            String user = controller.register(firstName.getText(), surname.getText(), nationalId.getText(),
                    address.getText(), phone.getText(), email.getText(), username.getText(),
                    password.getText(), confirm.getText(), employed.isSelected(),
                    companyName.getText(), companyAddress.getText(), jobTitle.getText());
            UiHelper.info(navigator.getStage(), "Registration successful",
                    "Your profile has been created. Log in as '" + user + "' to open your first account.");
            navigator.showLogin();
        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        return grid;
    }

    private static int addRow(GridPane grid, int row, String label, TextField field) {
        Label l = new Label(label);
        l.setMinWidth(170);
        field.setPrefWidth(320);
        grid.add(l, 0, row);
        grid.add(field, 1, row);
        return row + 1;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section");
        return label;
    }
}
