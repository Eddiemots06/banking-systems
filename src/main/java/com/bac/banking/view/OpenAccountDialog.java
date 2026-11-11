package com.bac.banking.view;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.exception.BankingException;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Boundary class for the Open Account use case. The dialog stays open and shows the
 * controller's message if a rule is broken (e.g. BWP 500 minimum for Investment).
 */
public class OpenAccountDialog extends Dialog<AccountSummary> {
    private AccountSummary created;

    public OpenAccountDialog(AccountController controller, Window owner) {
        initOwner(owner);
        setTitle("Open a new account");
        setHeaderText("Choose the type of account to open");

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll(controller.getAccountTypes());
        ComboBox<String> branch = new ComboBox<>();
        branch.getItems().addAll(controller.getBranches());
        branch.getSelectionModel().selectFirst();
        TextField deposit = new TextField();
        deposit.setPromptText("0.00");
        Label rules = new Label("Pick an account type to see its rules.");
        rules.setWrapText(true);
        rules.setMaxWidth(360);
        rules.getStyleClass().add("hint");
        Label error = UiHelper.errorLabel();
        error.setMaxWidth(360);

        type.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                rules.setText(controller.describeAccountType(value));
            }
        });
        type.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Account type"), 0, 0);
        grid.add(type, 1, 0);
        grid.add(new Label("Branch"), 0, 1);
        grid.add(branch, 1, 1);
        grid.add(new Label("Opening deposit (BWP)"), 0, 2);
        grid.add(deposit, 1, 2);
        grid.add(rules, 0, 3, 2, 1);
        grid.add(error, 0, 4, 2, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                created = controller.openAccount(type.getValue(), branch.getValue(), deposit.getText());
            } catch (BankingException | IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> button == ButtonType.OK ? created : null);
    }
}
