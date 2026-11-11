package com.bac.banking.dao;

import com.bac.banking.exception.DataAccessException;
import com.bac.banking.model.Transaction;
import com.bac.banking.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access for {@link Transaction} records.
 * Transactions are an audit trail, so they can be created, read and deleted but never edited.
 */
public class TransactionDAO implements DAO<Transaction, Long> {
    private static final String SELECT_BASE = "SELECT transaction_id, account_number, transaction_type, amount, "
            + "balance_after, transaction_time, reference FROM transactions ";

    private final DatabaseManager db;

    public TransactionDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Transaction create(Transaction transaction) {
        try (Connection con = db.getConnection()) {
            return insert(con, transaction);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save transaction", e);
        }
    }

    /** Inserts using an existing connection so callers can include it in a larger JDBC transaction. */
    Transaction insert(Connection con, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after, "
                + "transaction_time, reference) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getAccountNumber());
            ps.setString(2, t.getType().name());
            ps.setBigDecimal(3, t.getAmount());
            ps.setBigDecimal(4, t.getBalanceAfter());
            ps.setTimestamp(5, Timestamp.valueOf(t.getTimestamp()));
            ps.setString(6, t.getReference());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setTransactionId(keys.getLong(1));
                }
            }
            return t;
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE transaction_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load transaction " + id, e);
        }
    }

    /** Statement for one account, newest first. */
    public List<Transaction> findByAccount(String accountNumber) {
        return query(SELECT_BASE + "WHERE account_number = ? ORDER BY transaction_time DESC, transaction_id DESC",
                accountNumber);
    }

    @Override
    public List<Transaction> findAll() {
        return query(SELECT_BASE + "ORDER BY transaction_time DESC, transaction_id DESC", null);
    }

    /** Transactions are immutable once recorded. */
    @Override
    public boolean update(Transaction entity) {
        throw new UnsupportedOperationException("Transactions are an audit record and cannot be changed");
    }

    @Override
    public boolean delete(Long id) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM transactions WHERE transaction_id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete transaction " + id, e);
        }
    }

    private List<Transaction> query(String sql, String param) {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load transactions", e);
        }
    }

    private Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("transaction_id"),
                rs.getString("account_number"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_after"),
                rs.getTimestamp("transaction_time").toLocalDateTime(),
                rs.getString("reference"));
    }
}
