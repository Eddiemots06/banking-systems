package com.bac.banking.model;

import com.bac.banking.util.PasswordUtil;

/**
 * Abstract base for anyone who can log in to the system.
 * Holds the login credentials and the authentication behaviour shared by
 * {@link Customer} and {@link Administrator}.
 */
public abstract class User {
    private String username;
    private String passwordHash;
    private String salt;

    protected User(String username, String passwordHash, String salt) {
        setUsername(username);
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    /** Checks a plain-text password against the stored salted hash. */
    public boolean authenticate(String plainPassword) {
        if (plainPassword == null || passwordHash == null) {
            return false;
        }
        return PasswordUtil.matches(plainPassword, salt, passwordHash);
    }

    /** Replaces the password with a newly salted hash. */
    public void changePassword(String newPlainPassword) {
        this.salt = PasswordUtil.generateSalt();
        this.passwordHash = PasswordUtil.hash(newPlainPassword, salt);
    }

    /** Each subclass describes its role, e.g. "Customer" or "Administrator". */
    public abstract String getRole();

    /** Each subclass supplies the name shown on screen. */
    public abstract String getDisplayName();

    public String getUsername() {
        return username;
    }

    public final void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        this.username = username.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }
}
