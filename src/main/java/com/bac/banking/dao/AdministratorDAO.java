package com.bac.banking.dao;

import com.bac.banking.exception.DataAccessException;
import com.bac.banking.model.Administrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC CRUD for bank staff (administrators). */
public class AdministratorDAO implements DAO<Administrator, Long> {
    private static final String SELECT_BASE = "SELECT admin_id, username, full_name, password_hash, salt FROM administrators ";

    private final DatabaseManager db;

    public AdministratorDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Administrator create(Administrator admin) {
        String sql = "INSERT INTO administrators (username, full_name, password_hash, salt) VALUES (?, ?, ?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getFullName());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getSalt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    admin.setAdminId(keys.getLong(1));
                }
            }
            return admin;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save administrator", e);
        }
    }

    @Override
    public Optional<Administrator> findById(Long id) {
        return findOne(SELECT_BASE + "WHERE admin_id = ?", id);
    }

    public Optional<Administrator> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne(SELECT_BASE + "WHERE username = ?", username.trim().toLowerCase());
    }

    @Override
    public List<Administrator> findAll() {
        List<Administrator> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load administrators", e);
        }
    }

    @Override
    public boolean update(Administrator admin) {
        String sql = "UPDATE administrators SET username = ?, full_name = ?, password_hash = ?, salt = ? WHERE admin_id = ?";
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getFullName());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getSalt());
            ps.setLong(5, admin.getAdminId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update administrator", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM administrators WHERE admin_id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete administrator", e);
        }
    }

    private Optional<Administrator> findOne(String sql, Object param) {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load administrator", e);
        }
    }

    private Administrator map(ResultSet rs) throws SQLException {
        return new Administrator(rs.getLong("admin_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("password_hash"), rs.getString("salt"));
    }
}
