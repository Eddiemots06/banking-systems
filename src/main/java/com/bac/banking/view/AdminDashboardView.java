package com.bac.banking.view;

import com.bac.banking.controller.AdminController;
import com.bac.banking.controller.LoginController;
import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.controller.dto.InterestRunResult;
import com.bac.banking.exception.BankingException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Boundary class for the administrator: all accounts and the Pay Monthly Interest use case. */
public class AdminDashboardView {
    private final AdminController controller;
    private final LoginController loginController;
    private final ViewNavigator navigator;

    private final TableView<AccountSummary> table = new TableView<>();
    private final TextArea log = new TextArea();

    public AdminDashboardView(AdminController controller, LoginController loginController, ViewNavigator navigator) {
        this.controller = controller;
        this.loginController = loginController;
        this.navigator = navigator;
    }

    public Parent build() {
        Label title = UiHelper.heading(controller.getBankName() + " - Administrator: " + loginController.getLoggedInName());
        Button logout = new Button("Log out");
        logout.setOnAction(e -> {
            loginController.logout();
            navigator.showLogin();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, title, spacer, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");
        top.setPadding(new Insets(14, 20, 14, 20));

        table.getColumns().add(CustomerDashboardView.column("Account No.", AccountSummary::accountNumber, 110));
        table.getColumns().add(CustomerDashboardView.column("Customer", AccountSummary::ownerName, 170));
        table.getColumns().add(CustomerDashboardView.column("Type", AccountSummary::accountType, 150));
        table.getColumns().add(CustomerDashboardView.column("Branch", AccountSummary::branch, 110));
        table.getColumns().add(CustomerDashboardView.column("Balance", AccountSummary::balance, 140));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Button payInterest = new Button("Pay Monthly Interest");
        payInterest.getStyleClass().add("primary");
        payInterest.setOnAction(e -> payInterest());

        log.setEditable(false);
        log.setPrefRowCount(8);
        log.setPromptText("Interest run results appear here.");

        VBox centre = new VBox(12, UiHelper.heading("All accounts"), table,
                new Label("Savings: 0.05% per month   |   Investment: 5% per month   |   Cheque: no interest"),
                payInterest, log);
        centre.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);

        BorderPane root = new BorderPane(centre);
        root.setTop(top);
        root.getStyleClass().add("app-background");
        refresh();
        return root;
    }

    private void refresh() {
        try {
            table.setItems(FXCollections.observableArrayList(controller.getAllAccounts()));
        } catch (BankingException ex) {
            UiHelper.error(navigator.getStage(), ex.getMessage());
        }
    }

    private void payInterest() {
        if (!UiHelper.confirm(navigator.getStage(), "Pay monthly interest?",
                "This credits one month of interest to every Savings and Investment account.")) {
            return;
        }
        try {
            InterestRunResult result = controller.payMonthlyInterest();
            StringBuilder text = new StringBuilder();
            text.append("Paid: ").append(result.paid())
                    .append("   Skipped: ").append(result.skipped())
                    .append("   Failed: ").append(result.failed())
                    .append("   Total: ").append(result.totalPaid()).append('\n');
            result.details().forEach(line -> text.append(" - ").append(line).append('\n'));
            log.setText(text.toString());
            refresh();
        } catch (BankingException ex) {
            UiHelper.error(navigator.getStage(), ex.getMessage());
        }
    }
}
