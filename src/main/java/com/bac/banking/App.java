package com.bac.banking;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.AdminController;
import com.bac.banking.controller.CustomerController;
import com.bac.banking.controller.LoginController;
import com.bac.banking.controller.Session;
import com.bac.banking.dao.AccountDAO;
import com.bac.banking.dao.AdministratorDAO;
import com.bac.banking.dao.CustomerDAO;
import com.bac.banking.dao.DataSeeder;
import com.bac.banking.dao.DatabaseManager;
import com.bac.banking.dao.TransactionDAO;
import com.bac.banking.model.Bank;
import com.bac.banking.view.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point. Wires the four layers together:
 * DAOs (database) -> controllers (application logic) -> views (JavaFX GUI).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseManager db = DatabaseManager.fileDatabase();
        db.initialize();

        CustomerDAO customerDAO = new CustomerDAO(db);
        TransactionDAO transactionDAO = new TransactionDAO(db);
        AccountDAO accountDAO = new AccountDAO(db, customerDAO, transactionDAO);
        AdministratorDAO administratorDAO = new AdministratorDAO(db);
        new DataSeeder(customerDAO, accountDAO, administratorDAO).seedIfEmpty();

        Session session = new Session();
        Bank bank = new Bank("BAC Bank", "BACBBWGX");
        LoginController loginController = new LoginController(customerDAO, administratorDAO, session);
        CustomerController customerController = new CustomerController(customerDAO, accountDAO, session);
        AccountController accountController = new AccountController(customerDAO, accountDAO, transactionDAO, session);
        AdminController adminController = new AdminController(accountDAO, bank, session);

        stage.setMinWidth(820);
        stage.setMinHeight(560);
        new ViewNavigator(stage, loginController, customerController, accountController, adminController).showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
