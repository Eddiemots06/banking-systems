package com.bac.banking.dao;

import com.bac.banking.model.Account;
import com.bac.banking.model.AccountType;
import com.bac.banking.model.ChequeAccount;
import com.bac.banking.model.Customer;
import com.bac.banking.model.Employer;
import com.bac.banking.model.InvestmentAccount;
import com.bac.banking.model.SavingsAccount;
import com.bac.banking.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CRUD tests for the JDBC data access layer against a fresh in-memory H2 database. */
class DaoCrudTest {
    private CustomerDAO customerDAO;
    private AccountDAO accountDAO;
    private TransactionDAO transactionDAO;

    @BeforeEach
    void setUp() {
        DatabaseManager db = DatabaseManager.inMemory("dao-" + UUID.randomUUID());
        db.initialize();
        customerDAO = new CustomerDAO(db);
        transactionDAO = new TransactionDAO(db);
        accountDAO = new AccountDAO(db, customerDAO, transactionDAO);
    }

    private Customer newCustomer(String username, String omang, boolean employed) {
        Customer c = new Customer(null, "Test", "User", omang, "Gaborone", "71111111", "t@mail.bw",
                username, "hash", "salt");
        if (employed) {
            c.setEmployer(new Employer("Acme", "Gaborone", "Clerk"));
        }
        return customerDAO.create(c);
    }

    @Test
    @DisplayName("Customer CRUD including employer details")
    void customerCrud() {
        Customer created = newCustomer("tuser", "111111111", true);
        assertNotNull(created.getCustomerId());

        Customer read = customerDAO.findByUsername("TUSER").orElseThrow();
        assertEquals("Acme", read.getEmployer().getCompanyName());

        read.setAddress("Francistown");
        read.setEmployer(null);
        assertTrue(customerDAO.update(read));
        Customer updated = customerDAO.findById(read.getCustomerId()).orElseThrow();
        assertEquals("Francistown", updated.getAddress());
        assertFalse(updated.isEmployed());

        assertTrue(customerDAO.delete(updated.getCustomerId()));
        assertTrue(customerDAO.findById(updated.getCustomerId()).isEmpty());
    }

    @Test
    @DisplayName("Account CRUD rebuilds the correct subclass")
    void accountCrudAndPolymorphicLoad() throws Exception {
        Customer owner = newCustomer("owner", "222222222", true);
        SavingsAccount savings = new SavingsAccount(accountDAO.nextAccountNumber(AccountType.SAVINGS), owner, "Main Mall");
        savings.deposit(new BigDecimal("100"));
        accountDAO.create(savings);
        accountDAO.create(new InvestmentAccount(accountDAO.nextAccountNumber(AccountType.INVESTMENT), owner,
                "Main Mall", new BigDecimal("500")));
        accountDAO.create(new ChequeAccount(accountDAO.nextAccountNumber(AccountType.CHEQUE), owner, "Main Mall"));

        Customer fresh = customerDAO.findById(owner.getCustomerId()).orElseThrow();
        List<Account> accounts = accountDAO.findByCustomer(fresh);
        assertEquals(3, accounts.size());
        assertTrue(accounts.get(0) instanceof SavingsAccount);
        assertTrue(accounts.get(1) instanceof InvestmentAccount);
        assertTrue(accounts.get(2) instanceof ChequeAccount);
        assertEquals(new BigDecimal("100.00"), accounts.get(0).getBalance());

        Account loaded = accountDAO.findById(savings.getAccountNumber()).orElseThrow();
        loaded.setBranch("Game City");
        assertTrue(accountDAO.update(loaded));
        assertEquals("Game City", accountDAO.findById(savings.getAccountNumber()).orElseThrow().getBranch());

        assertEquals(1, transactionDAO.findByAccount(savings.getAccountNumber()).size());
        assertTrue(accountDAO.delete(savings.getAccountNumber()));
        assertTrue(accountDAO.findById(savings.getAccountNumber()).isEmpty());
        assertEquals(0, transactionDAO.findByAccount(savings.getAccountNumber()).size(), "cascade delete");
    }

    @Test
    @DisplayName("applyTransaction saves balance and transaction together")
    void applyTransactionIsAtomic() throws Exception {
        Customer owner = newCustomer("atomic", "333333333", false);
        SavingsAccount savings = new SavingsAccount(accountDAO.nextAccountNumber(AccountType.SAVINGS), owner, "Maun");
        accountDAO.create(savings);

        Transaction tx = savings.deposit(new BigDecimal("75.25"));
        accountDAO.applyTransaction(savings, tx);
        assertNotNull(tx.getTransactionId());
        assertEquals(new BigDecimal("75.25"),
                accountDAO.findById(savings.getAccountNumber()).orElseThrow().getBalance());

        // An account that is not in the database: nothing may be written.
        Customer other = newCustomer("ghost", "444444444", false);
        SavingsAccount ghost = new SavingsAccount("19999999", other, "Maun");
        Transaction lost = ghost.deposit(new BigDecimal("10"));
        assertThrows(com.bac.banking.exception.DataAccessException.class,
                () -> accountDAO.applyTransaction(ghost, lost));
        assertEquals(0, transactionDAO.findByAccount("19999999").size(), "rolled back");
    }

    @Test
    @DisplayName("Database refuses duplicate usernames and negative balances")
    void constraints() {
        newCustomer("dup", "555555555", false);
        assertThrows(com.bac.banking.exception.DataAccessException.class,
                () -> newCustomer("dup", "666666666", false));
        assertThrows(UnsupportedOperationException.class, () -> transactionDAO.update(null));
    }

    @Test
    @DisplayName("Seeder inserts 10 customers with accounts exactly once")
    void seeder() {
        DatabaseManager db = DatabaseManager.inMemory("seed-" + UUID.randomUUID());
        db.initialize();
        CustomerDAO c = new CustomerDAO(db);
        TransactionDAO t = new TransactionDAO(db);
        AccountDAO a = new AccountDAO(db, c, t);
        AdministratorDAO admin = new AdministratorDAO(db);
        assertTrue(new DataSeeder(c, a, admin).seedIfEmpty());
        assertFalse(new DataSeeder(c, a, admin).seedIfEmpty());
        assertEquals(10, c.findAll().size());
        assertEquals(16, a.findAll().size());
        assertTrue(t.findAll().size() >= 16);
        assertTrue(admin.findByUsername("admin").orElseThrow().authenticate(DataSeeder.ADMIN_PASSWORD));
    }
}
