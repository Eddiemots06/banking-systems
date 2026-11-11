package com.bac.banking.model;

/**
 * A bank staff member who can view all accounts and run the monthly interest payment.
 */
public class Administrator extends User {
    private Long adminId;
    private String fullName;

    public Administrator(Long adminId, String username, String fullName, String passwordHash, String salt) {
        super(username, passwordHash, salt);
        this.adminId = adminId;
        this.fullName = fullName;
    }

    @Override
    public String getRole() {
        return "Administrator";
    }

    @Override
    public String getDisplayName() {
        return fullName;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getFullName() {
        return fullName;
    }
}
