package com.bac.banking.integration;

import com.bac.banking.controller.AccountController;
import com.bac.banking.controller.AdminController;
import com.bac.banking.controller.CustomerController;
import com.bac.banking.controller.LoginController;
import com.bac.banking.controller.Session;
import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.controller.dto.InterestRunResult;
import com.bac.banking.controller.dto.TransactionRow;
import com.bac.banking.dao.AccountDAO;
import com.bac.banking.dao.AdministratorDAO;
import com.bac.banking.dao.CustomerDAO;
import com.bac.banking.dao.DataSeeder;
import com.bac.banking.dao.DatabaseManager;
import com.bac.banking.dao.TransactionDAO;
import com.bac.banking.exception.BankingException;
import com.bac.banking.exception.InsufficientFundsException;
import com.bac.banking.exception.ValidationException;
import com.bac.banking.model.Account;
import com.bac.banking.model.Bank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests: each test drives the controllers exactly as the JavaFX views do
 * (passing raw text from the form fields), and then checks the result in the database
 * using separate, freshly created DAOs. This covers View input -> Controller ->
 * Domain model -> JDBC DAO -> H2 database.
 */
class BankingSystemIntegrationTest {
    private DatabaseManager db;
    private LoginController login;
    private CustomerController customers;
    private AccountController accounts;
    private AdminController admin;

    @BeforeEach
    void wireApplication() {
        db = DatabaseManager.inMemory("it-" + UUID.randomUUID());
        db.initialize();
        CustomerDAO customerDAO = new CustomerDAO(db);
        TransactionDAO transactionDAO = new TransactionDAO(db);
        AccountDAO accountDAO = new AccountDAO(db, customerDAO, transactionDAO);
        AdministratorDAO administratorDAO = new AdministratorDAO(db);
        new DataSeeder(customerDAO, accountDAO, administratorDAO).seedIfEmpty();

        Session session = new Session();
        login = new LoginController(customerDAO, administratorDAO, session);
        customers = new CustomerController(customerDAO, accountDAO, session);
        accounts = new AccountController(customerDAO, accountDAO, transactionDAO, session);
        admin = new AdminController(accountDAO, new Bank("BAC Bank", "BACBBWGX"), session);
    }

    /** Reads the balance straight from the database with new DAO objects. */
    private BigDecimal balanceInDatabase(String accountNumber) {
        CustomerDAO c = new CustomerDAO(db);
        AccountDAO a = new AccountDAO(db, c, new TransactionDAO(db));
        return a.findById(accountNumber).map(Account::getBalance).orElseThrow();
    }

    @Test
    @DisplayName("IT-01 Full flow: register -> login -> open 3 accounts -> deposit -> withdraw -> statement")
    void fullCustomerJourney() throws BankingException {
        String username = customers.register("Amantle", "Sebina", "987654321", "Plot 10, Gaborone",
                "71999999", "amantle@mail.bw", "asebina", "Secret123", "Secret123",
                true, "Kalahari Mining Ltd", "Main Mall, Gaborone", "Geologist");
        assertEquals("asebina", username);

        assertEquals(Session.Role.CUSTOMER, login.login("asebina", "Secret123"));
        assertEquals("Amantle Sebina", login.getLoggedInName());

        AccountSummary savings = accounts.openAccount("Savings Account", "Main Mall", "");
        AccountSummary investment = accounts.openAccount("Investment Account", "Main Mall", "1500");
        AccountSummary cheque = accounts.openAccount("Cheque Account", "Main Mall", "0");
        assertEquals(3, accounts.getMyAccounts().size());

        accounts.deposit(savings.accountNumber(), "1,000.00", "Cash");
        accounts.deposit(cheque.accountNumber(), "9500", "Salary October");
        AccountSummary afterWithdraw = accounts.withdraw(investment.accountNumber(), "250.50", "School fees");
        assertEquals("BWP 1,249.50", afterWithdraw.balance());

        assertEquals(new BigDecimal("1000.00"), balanceInDatabase(savings.accountNumber()));
        assertEquals(new BigDecimal("1249.50"), balanceInDatabase(investment.accountNumber()));
        assertEquals(new BigDecimal("9500.00"), balanceInDatabase(cheque.accountNumber()));

        List<TransactionRow> statement = accounts.getStatement(investment.accountNumber());
        assertEquals(2, statement.size());
        assertEquals("Withdrawal", statement.get(0).type());
        assertEquals("Opening deposit", statement.get(1).type());
        assertEquals("BWP 11,749.50", accounts.getTotalBalance());
    }

    @Test
    @DisplayName("IT-02 Business rules are enforced end to end and nothing is saved on failure")
    void rulesEnforced() throws BankingException {
        login.login("lkgosi", DataSeeder.DEMO_PASSWORD); // seeded student, not employed, savings only
        String savings = accounts.getMyAccounts().get(0).accountNumber();
        BigDecimal before = balanceInDatabase(savings);

        assertThrows(ValidationException.class, () -> accounts.withdraw(savings, "10", null));
        assertThrows(ValidationException.class, () -> accounts.openAccount("Cheque Account", "Molepolole", "0"));
        assertThrows(ValidationException.class, () -> accounts.openAccount("Investment Account", "Molepolole", "499"));
        assertThrows(ValidationException.class, () -> accounts.deposit(savings, "abc", null));
        assertThrows(ValidationException.class, () -> accounts.deposit(savings, "-20", null));

        assertEquals(before, balanceInDatabase(savings));
        assertEquals(1, accounts.getMyAccounts().size());

        customers.updateEmployment("Molepolole Hardware", "Main Road, Molepolole", "Cashier");
        accounts.openAccount("Cheque Account", "Molepolole", "0");
        assertEquals(2, accounts.getMyAccounts().size(), "cheque account allowed once employed");
    }

    @Test
    @DisplayName("IT-03 Overdraft is refused and the database balance is unchanged")
    void overdraftRefused() throws BankingException {
        login.login("mtau", DataSeeder.DEMO_PASSWORD); // investment with BWP 500
        String inv = accounts.getMyAccounts().get(0).accountNumber();
        assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(inv, "500.01", null));
        assertEquals(new BigDecimal("500.00"), balanceInDatabase(inv));
    }

    @Test
    @DisplayName("IT-04 Login security: wrong password, lock-out and role-based access")
    void loginSecurity() throws BankingException {
        assertThrows(ValidationException.class, () -> login.login("kmolefe", "wrong"));
        assertThrows(ValidationException.class, () -> login.login("nobody", "wrong"));
        assertThrows(ValidationException.class, () -> login.login("", ""));
        assertEquals(Session.Role.CUSTOMER, login.login("kmolefe", DataSeeder.DEMO_PASSWORD));
        assertThrows(ValidationException.class, admin::payMonthlyInterest, "customer cannot pay interest");

        login.logout();
        assertThrows(ValidationException.class, accounts::getMyAccounts, "logged out user sees nothing");
        for (int i = 0; i < 3; i++) {
            assertThrows(ValidationException.class, () -> login.login("kmolefe", "bad"));
        }
        ValidationException locked = assertThrows(ValidationException.class,
                () -> login.login("kmolefe", DataSeeder.DEMO_PASSWORD));
        assertTrue(locked.getMessage().contains("Too many"));
    }

    @Test
    @DisplayName("IT-05 Customer cannot touch another customer's account")
    void ownershipEnforced() throws BankingException {
        login.login("nseretse", DataSeeder.DEMO_PASSWORD);
        String neosAccount = accounts.getMyAccounts().get(0).accountNumber();
        login.logout();
        login.login("kmolefe", DataSeeder.DEMO_PASSWORD);
        assertThrows(ValidationException.class, () -> accounts.deposit(neosAccount, "10", null));
        assertThrows(ValidationException.class, () -> accounts.getStatement(neosAccount));
    }

    @Test
    @DisplayName("IT-06 Admin pays monthly interest: 5% investment, 0.05% savings, none on cheque")
    void adminPaysInterest() throws BankingException {
        login.login("nseretse", DataSeeder.DEMO_PASSWORD); // has all three account types
        List<AccountSummary> neo = accounts.getMyAccounts();
        String sav = find(neo, "Savings Account");
        String inv = find(neo, "Investment Account");
        String chq = find(neo, "Cheque Account");
        BigDecimal chequeBefore = balanceInDatabase(chq);
        login.logout();

        assertEquals(Session.Role.ADMINISTRATOR, login.login("admin", DataSeeder.ADMIN_PASSWORD));
        assertEquals(16, admin.getAllAccounts().size());
        InterestRunResult result = admin.payMonthlyInterest();
        assertEquals(10, result.paid(), "5 savings + 5 investment accounts in the seed data");
        assertEquals(0, result.failed());

        assertEquals(new BigDecimal("1200.60"), balanceInDatabase(sav));   // 1200 * 0.0005 = 0.60
        assertEquals(new BigDecimal("15750.00"), balanceInDatabase(inv));  // 15000 * 0.05 = 750
        assertEquals(chequeBefore, balanceInDatabase(chq));
        login.logout();

        login.login("nseretse", DataSeeder.DEMO_PASSWORD);
        assertEquals("Interest", accounts.getStatement(inv).get(0).type());
        assertFalse(accounts.getStatement(chq).stream().anyMatch(r -> r.type().equals("Interest")));
    }

    @Test
    @DisplayName("IT-07 Registration validation and duplicate detection")
    void registrationValidation() {
        assertThrows(ValidationException.class, () -> customers.register("A", "B", "12345", "Addr", null, null,
                "abc", "Secret123", "Secret123", false, null, null, null));          // Omang not 9 digits
        assertThrows(ValidationException.class, () -> customers.register("A", "B", "123123123", "Addr", null, null,
                "abc", "short", "short", false, null, null, null));                  // weak password
        assertThrows(ValidationException.class, () -> customers.register("A", "B", "123123123", "Addr", null, null,
                "kmolefe", "Secret123", "Secret123", false, null, null, null));      // username taken
        assertThrows(ValidationException.class, () -> customers.register("A", "B", "123123123", "Addr", null, null,
                "abc", "Secret123", "Secret123", true, "", "", null));               // employer missing
    }

    private static String find(List<AccountSummary> list, String type) {
        return list.stream().filter(a -> a.accountType().equals(type)).findFirst().orElseThrow().accountNumber();
    }
}
