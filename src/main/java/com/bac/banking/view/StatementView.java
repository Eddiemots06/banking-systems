package com.bac.banking.view;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.dto.TransactionRow;
import com.bac.banking.exception.BankingException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Boundary class for the View Transaction History use case. */
public class StatementView {
    private final AccountController controller;
    private final ViewNavigator navigator;
    private final String accountNumber;

    public StatementView(AccountController controller, ViewNavigator navigator, String accountNumber) {
        this.controller = controller;
        this.navigator = navigator;
        this.accountNumber = accountNumber;
    }

    public Parent build() {
        TableView<TransactionRow> table = new TableView<>();
        table.getColumns().add(CustomerDashboardView.column("Date", TransactionRow::dateTime, 150));
        table.getColumns().add(CustomerDashboardView.column("Type", TransactionRow::type, 130));
        table.getColumns().add(CustomerDashboardView.column("Reference", TransactionRow::reference, 220));
        table.getColumns().add(CustomerDashboardView.column("Amount", TransactionRow::amount, 130));
        table.getColumns().add(CustomerDashboardView.column("Balance after", TransactionRow::balanceAfter, 140));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No transactions yet."));
        try {
            table.setItems(FXCollections.observableArrayList(controller.getStatement(accountNumber)));
        } catch (BankingException ex) {
            UiHelper.error(navigator.getStage(), ex.getMessage());
        }

        Button back = new Button("Back to my accounts");
        back.setOnAction(e -> navigator.showCustomerDashboard());

        VBox root = new VBox(12, UiHelper.heading("Statement for account " + accountNumber), table, back);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("app-background");
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }
}
