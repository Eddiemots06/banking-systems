package com.bac.banking.view;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.exception.BankingException;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Boundary class for the Deposit Funds and Withdraw Funds use cases.
 * The amount is passed to the controller as text; parsing and rules are handled there.
 */
public class TransactionDialog extends Dialog<AccountSummary> {
    private AccountSummary result;

    public TransactionDialog(AccountController controller, Window owner, AccountSummary account, boolean isDeposit) {
        initOwner(owner);
        String action = isDeposit ? "Deposit" : "Withdraw";
        setTitle(action);
        setHeaderText(action + (isDeposit ? " into " : " from ") + account.accountType() + " "
                + account.accountNumber() + "\nCurrent balance: " + account.balance());

        TextField amount = new TextField();
        amount.setPromptText("e.g. 250.00");
        TextField reference = new TextField();
        reference.setPromptText(isDeposit ? "e.g. Cash deposit" : "e.g. Rent");
        Label error = UiHelper.errorLabel();
        error.setMaxWidth(340);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Amount (BWP)"), 0, 0);
        grid.add(amount, 1, 0);
        grid.add(new Label("Reference"), 0, 1);
        grid.add(reference, 1, 1);
        grid.add(error, 0, 2, 2, 1);
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                result = isDeposit
                        ? controller.deposit(account.accountNumber(), amount.getText(), reference.getText())
                        : controller.withdraw(account.accountNumber(), amount.getText(), reference.getText());
            } catch (BankingException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> button == ButtonType.OK ? result : null);
    }
}
