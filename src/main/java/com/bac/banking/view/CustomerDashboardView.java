package com.bac.banking.view;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.CustomerController;
import com.bac.banking.controller.LoginController;
import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.controller.dto.CustomerProfile;
import com.bac.banking.exception.BankingException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Function;

/**
 * Boundary class showing the customer's accounts with buttons for each account use case.
 * Every action is delegated to a controller; the view only displays results.
 */
public class CustomerDashboardView {
    private final AccountController accountController;
    private final CustomerController customerController;
    private final LoginController loginController;
    private final ViewNavigator navigator;

    private final TableView<AccountSummary> table = new TableView<>();
    private final Label totalLabel = new Label();
    private final Label rulesLabel = new Label("Select an account to see its rules.");
    private final Label employmentLabel = new Label();
    private final Button depositButton = new Button("Deposit");
    private final Button withdrawButton = new Button("Withdraw");
    private final Button statementButton = new Button("Statement");

    public CustomerDashboardView(AccountController accountController, CustomerController customerController,
                                 LoginController loginController, ViewNavigator navigator) {
        this.accountController = accountController;
        this.customerController = customerController;
        this.loginController = loginController;
        this.navigator = navigator;
    }

    public Parent build() {
        Label welcome = UiHelper.heading("Welcome, " + loginController.getLoggedInName());
        Button logout = new Button("Log out");
        logout.setOnAction(e -> {
            loginController.logout();
            navigator.showLogin();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, welcome, spacer, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");
        top.setPadding(new Insets(14, 20, 14, 20));

        table.getColumns().add(column("Account No.", AccountSummary::accountNumber, 120));
        table.getColumns().add(column("Type", AccountSummary::accountType, 160));
        table.getColumns().add(column("Branch", AccountSummary::branch, 120));
        table.getColumns().add(column("Opened", AccountSummary::dateOpened, 110));
        table.getColumns().add(column("Balance", AccountSummary::balance, 150));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("You have no accounts yet. Click 'Open Account' to get started."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> onSelect(selected));

        Button openButton = new Button("Open Account");
        openButton.getStyleClass().add("primary");
        openButton.setOnAction(e -> openAccount());
        depositButton.setOnAction(e -> transact(true));
        withdrawButton.setOnAction(e -> transact(false));
        statementButton.setOnAction(e -> {
            AccountSummary selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigator.showStatement(selected.accountNumber());
            }
        });
        Button employmentButton = new Button("Update Employment");
        employmentButton.setOnAction(e -> updateEmployment());

        HBox actions = new HBox(10, openButton, depositButton, withdrawButton, statementButton, employmentButton);
        rulesLabel.setWrapText(true);
        rulesLabel.getStyleClass().add("hint");
        totalLabel.getStyleClass().add("total");
        employmentLabel.getStyleClass().add("hint");

        VBox centre = new VBox(12, UiHelper.heading("My Accounts"), table, rulesLabel, actions, totalLabel,
                employmentLabel);
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
            table.setItems(FXCollections.observableArrayList(accountController.getMyAccounts()));
            totalLabel.setText("Total across all accounts: " + accountController.getTotalBalance());
            CustomerProfile profile = customerController.getProfile();
            employmentLabel.setText(profile.employed()
                    ? "Employer on record: " + profile.companyName() + ", " + profile.companyAddress()
                    : "No employer on record - add one with 'Update Employment' to open a Cheque Account.");
        } catch (BankingException ex) {
            UiHelper.error(navigator.getStage(), ex.getMessage());
        }
        onSelect(null);
    }

    private void onSelect(AccountSummary selected) {
        boolean has = selected != null;
        depositButton.setDisable(!has);
        statementButton.setDisable(!has);
        withdrawButton.setDisable(!has || !selected.canWithdraw());
        rulesLabel.setText(has ? selected.accountType() + ": " + selected.rules()
                : "Select an account to deposit, withdraw or view its statement.");
    }

    private void openAccount() {
        OpenAccountDialog dialog = new OpenAccountDialog(accountController, navigator.getStage());
        Optional<AccountSummary> created = dialog.showAndWait();
        if (created.isPresent()) {
            UiHelper.info(navigator.getStage(), "Account opened",
                    created.get().accountType() + " " + created.get().accountNumber()
                            + " is ready. Balance: " + created.get().balance());
            refresh();
        }
    }

    private void transact(boolean isDeposit) {
        AccountSummary selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        TransactionDialog dialog = new TransactionDialog(accountController, navigator.getStage(), selected, isDeposit);
        Optional<AccountSummary> updated = dialog.showAndWait();
        if (updated.isPresent()) {
            UiHelper.info(navigator.getStage(), isDeposit ? "Deposit successful" : "Withdrawal successful",
                    "New balance on " + updated.get().accountNumber() + ": " + updated.get().balance());
            refresh();
        }
    }

    private void updateEmployment() {
        try {
            EmploymentDialog dialog = new EmploymentDialog(customerController, navigator.getStage(),
                    customerController.getProfile());
            if (dialog.showAndWait().orElse(false)) {
                refresh();
            }
        } catch (BankingException ex) {
            UiHelper.error(navigator.getStage(), ex.getMessage());
        }
    }

    static <S> TableColumn<S, String> column(String title, Function<S, String> getter, double width) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}
