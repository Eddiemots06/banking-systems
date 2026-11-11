package com.bac.banking.dao;

import com.bac.banking.exception.DataAccessException;
import com.bac.banking.model.Customer;
import com.bac.banking.model.Employer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC CRUD for {@link Customer} and their optional {@link Employer} details.
 * Accounts are loaded separately by {@link AccountDAO}.
 */
public class CustomerDAO implements DAO<Customer, Long> {
    private static final String SELECT_BASE =
            "SELECT c.customer_id, c.first_name, c.surname, c.national_id, c.address, c.phone, c.email, "
                    + "c.username, c.password_hash, c.salt, "
                    + "e.company_name, e.company_address, e.job_title "
                    + "FROM customers c LEFT JOIN employers e ON e.customer_id = c.customer_id ";

    private final DatabaseManager db;

    public CustomerDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Customer create(Customer customer) {
        String sql = "INSERT INTO customers (first_name, surname, national_id, address, phone, email, "
                + "username, password_hash, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, customer.getFirstName());
                ps.setString(2, customer.getSurname());
                ps.setString(3, customer.getNationalId());
                ps.setString(4, customer.getAddress());
                ps.setString(5, customer.getPhone());
                ps.setString(6, customer.getEmail());
                ps.setString(7, customer.getUsername());
                ps.setString(8, customer.getPasswordHash());
                ps.setString(9, customer.getSalt());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        customer.setCustomerId(keys.getLong(1));
                    }
                }
                if (customer.getEmployer() != null) {
                    insertEmployer(con, customer.getCustomerId(), customer.getEmployer());
                }
                con.commit();
                return customer;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save customer " + customer.getUsername(), e);
        }
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return findOne(SELECT_BASE + "WHERE c.customer_id = ?", id);
    }

    public Optional<Customer> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne(SELECT_BASE + "WHERE c.username = ?", username.trim().toLowerCase());
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "ORDER BY c.surname, c.first_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add(map(rs));
            }
            return customers;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load customers", e);
        }
    }

    @Override
    public boolean update(Customer customer) {
        String sql = "UPDATE customers SET first_name = ?, surname = ?, national_id = ?, address = ?, phone = ?, "
                + "email = ?, username = ?, password_hash = ?, salt = ? WHERE customer_id = ?";
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, customer.getFirstName());
                ps.setString(2, customer.getSurname());
                ps.setString(3, customer.getNationalId());
                ps.setString(4, customer.getAddress());
                ps.setString(5, customer.getPhone());
                ps.setString(6, customer.getEmail());
                ps.setString(7, customer.getUsername());
                ps.setString(8, customer.getPasswordHash());
                ps.setString(9, customer.getSalt());
                ps.setLong(10, customer.getCustomerId());
                int rows = ps.executeUpdate();
                try (PreparedStatement del = con.prepareStatement("DELETE FROM employers WHERE customer_id = ?")) {
                    del.setLong(1, customer.getCustomerId());
                    del.executeUpdate();
                }
                if (customer.getEmployer() != null) {
                    insertEmployer(con, customer.getCustomerId(), customer.getEmployer());
                }
                con.commit();
                return rows == 1;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update customer " + customer.getCustomerId(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete customer " + id, e);
        }
    }

    public boolean usernameExists(String username) {
        return exists("SELECT 1 FROM customers WHERE username = ?", username.trim().toLowerCase());
    }

    public boolean nationalIdExists(String nationalId) {
        return exists("SELECT 1 FROM customers WHERE national_id = ?", nationalId.trim());
    }

    public int count() {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count customers", e);
        }
    }

    // ---------- helpers ----------

    private void insertEmployer(Connection con, long customerId, Employer employer) throws SQLException {
        String sql = "INSERT INTO employers (customer_id, company_name, company_address, job_title) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setString(2, employer.getCompanyName());
            ps.setString(3, employer.getCompanyAddress());
            ps.setString(4, employer.getJobTitle());
            ps.executeUpdate();
        }
    }

    private Optional<Customer> findOne(String sql, Object param) {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load customer", e);
        }
    }

    private boolean exists(String sql, String param) {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check customer", e);
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getLong("customer_id"),
                rs.getString("first_name"),
                rs.getString("surname"),
                rs.getString("national_id"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("salt"));
        String companyName = rs.getString("company_name");
        if (companyName != null) {
            customer.setEmployer(new Employer(companyName, rs.getString("company_address"), rs.getString("job_title")));
        }
        return customer;
    }
}
