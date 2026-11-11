package com.bac.banking.controller;

/**
 * Remembers who is logged in. Only ids and names are kept: controllers reload fresh
 * data from the database for every request so balances are never stale.
 */
public class Session {

    public enum Role { CUSTOMER, ADMINISTRATOR }

    private Long userId;
    private String username;
    private String displayName;
    private Role role;

    void start(Long userId, String username, String displayName, Role role) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public void clear() {
        userId = null;
        username = null;
        displayName = null;
        role = null;
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public boolean isAdministrator() {
        return role == Role.ADMINISTRATOR;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }
}
