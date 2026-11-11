package com.bac.banking.view;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.AdminController;
import com.bac.banking.controller.CustomerController;
import com.bac.banking.controller.LoginController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Switches between screens on the single application window.
 * Holds the controllers so each view receives only the controller(s) it talks to.
 */
public class ViewNavigator {
    private final Stage stage;
    private final LoginController loginController;
    private final CustomerController customerController;
    private final AccountController accountController;
    private final AdminController adminController;

    public ViewNavigator(Stage stage, LoginController loginController, CustomerController customerController,
                         AccountController accountController, AdminController adminController) {
        this.stage = stage;
        this.loginController = loginController;
        this.customerController = customerController;
        this.accountController = accountController;
        this.adminController = adminController;
    }

    public void showLogin() {
        show(new LoginView(loginController, this).build(), "Login");
    }

    public void showRegister() {
        show(new RegisterView(customerController, this).build(), "Register");
    }

    public void showCustomerDashboard() {
        show(new CustomerDashboardView(accountController, customerController, loginController, this).build(),
                "My Accounts");
    }

    public void showStatement(String accountNumber) {
        show(new StatementView(accountController, this, accountNumber).build(), "Statement " + accountNumber);
    }

    public void showAdminDashboard() {
        show(new AdminDashboardView(adminController, loginController, this).build(), "Administrator");
    }

    public Stage getStage() {
        return stage;
    }

    private void show(Parent root, String title) {
        Scene scene = new Scene(root, 980, 640);
        URL css = ViewNavigator.class.getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setTitle("BAC Bank - " + title);
        stage.setScene(scene);
        stage.show();
    }
}
