package com.bac.banking.dao;

import com.bac.banking.exception.DataAccessException;
import com.bac.banking.model.Account;
import com.bac.banking.model.AccountType;
import com.bac.banking.model.ChequeAccount;
import com.bac.banking.model.Customer;
import com.bac.banking.model.InvestmentAccount;
import com.bac.banking.model.SavingsAccount;
import com.bac.banking.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC CRUD for the {@link Account} hierarchy, stored in one table with an
 * account_type column. Rows are turned back into the correct subclass when read.
 */
public class AccountDAO implements DAO<Account, String> {
    private static final String SELECT_BASE =
            "SELECT account_number, customer_id, account_type, branch, balance, date_opened FROM accounts ";

    private final DatabaseManager db;
    private final CustomerDAO customerDAO;
    private final TransactionDAO transactionDAO;

    public AccountDAO(DatabaseManager db, CustomerDAO customerDAO, TransactionDAO transactionDAO) {
        this.db = db;
        this.customerDAO = customerDAO;
        this.transactionDAO = transactionDAO;
    }

    /** Next free account number; the first digit shows the type (1 savings, 2 investment, 3 cheque). */
    public String nextAccountNumber(AccountType type) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT NEXT VALUE FOR account_number_seq");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int prefix = type.ordinal() + 1;
            return prefix + String.valueOf(rs.getLong(1));
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate an account number", e);
        }
    }

    /** Inserts the account and any transactions already on it (e.g. the opening deposit) atomically. */
    @Override
    public Account create(Account account) {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, branch, balance, date_opened) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, account.getAccountNumber());
                ps.setLong(2, account.getOwner().getCustomerId());
                ps.setString(3, account.getAccountType().name());
                ps.setString(4, account.getBranch());
                ps.setBigDecimal(5, account.getBalance());
                ps.setDate(6, Date.valueOf(account.getDateOpened()));
                ps.executeUpdate();
                for (Transaction t : account.getTransactions()) {
                    if (t.getTransactionId() == null) {
                        transactionDAO.insert(con, t);
                    }
                }
                con.commit();
                return account;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save account " + account.getAccountNumber(), e);
        }
    }

    @Override
    public Optional<Account> findById(String accountNumber) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT customer_id FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                long customerId = rs.getLong(1);
                Optional<Customer> owner = customerDAO.findById(customerId);
                if (owner.isEmpty()) {
                    return Optional.empty();
                }
                findByCustomer(owner.get());
                return owner.get().findAccount(accountNumber);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load account " + accountNumber, e);
        }
    }

    /**
     * Loads every account of the customer and attaches it to that customer object.
     * Accounts already attached are reused rather than duplicated.
     */
    public List<Account> findByCustomer(Customer customer) {
        List<Account> accounts = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE customer_id = ? ORDER BY account_number")) {
            ps.setLong(1, customer.getCustomerId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(map(rs, customer));
                }
            }
            return accounts;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load accounts for customer " + customer.getCustomerId(), e);
        }
    }

    /** Every account in the bank, each attached to its owner (used by the administrator). */
    @Override
    public List<Account> findAll() {
        List<Account> all = new ArrayList<>();
        for (Customer customer : customerDAO.findAll()) {
            all.addAll(findByCustomer(customer));
        }
        return all;
    }

    @Override
    public boolean update(Account account) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE accounts SET branch = ?, balance = ? WHERE account_number = ?")) {
            ps.setString(1, account.getBranch());
            ps.setBigDecimal(2, account.getBalance());
            ps.setString(3, account.getAccountNumber());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update account " + account.getAccountNumber(), e);
        }
    }

    /**
     * Saves a new balance and the transaction that caused it in ONE database transaction,
     * so the balance and the statement can never disagree.
     */
    public void applyTransaction(Account account, Transaction transaction) {
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET balance = ? WHERE account_number = ?")) {
                ps.setBigDecimal(1, account.getBalance());
                ps.setString(2, account.getAccountNumber());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Account " + account.getAccountNumber() + " not found");
                }
                transactionDAO.insert(con, transaction);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not record transaction on " + account.getAccountNumber(), e);
        }
    }

    @Override
    public boolean delete(String accountNumber) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete account " + accountNumber, e);
        }
    }

    /** Polymorphic mapping: builds the right subclass for each account_type value. */
    private Account map(ResultSet rs, Customer owner) throws SQLException {
        String number = rs.getString("account_number");
        Optional<Account> existing = owner.findAccount(number);
        if (existing.isPresent()) {
            return existing.get();
        }
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        String branch = rs.getString("branch");
        BigDecimal balance = rs.getBigDecimal("balance");
        LocalDate opened = rs.getDate("date_opened").toLocalDate();
        return switch (type) {
            case SAVINGS -> new SavingsAccount(number, owner, branch, balance, opened);
            case INVESTMENT -> new InvestmentAccount(number, owner, branch, balance, opened);
            case CHEQUE -> new ChequeAccount(number, owner, branch, balance, opened);
        };
    }
}
