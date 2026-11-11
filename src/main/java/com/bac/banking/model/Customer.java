package com.bac.banking.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A person who banks with us. A customer can hold one or many accounts of any type
 * (e.g. a savings and an investment account, or all three).
 */
public class Customer extends User {
    private Long customerId;
    private String firstName;
    private String surname;
    private String nationalId;
    private String address;
    private String phone;
    private String email;
    private Employer employer;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(Long customerId, String firstName, String surname, String nationalId,
                    String address, String phone, String email,
                    String username, String passwordHash, String salt) {
        super(username, passwordHash, salt);
        this.customerId = customerId;
        setFirstName(firstName);
        setSurname(surname);
        setNationalId(nationalId);
        setAddress(address);
        this.phone = phone;
        this.email = email;
    }

    @Override
    public String getRole() {
        return "Customer";
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + surname;
    }

    /** Called by the Account constructor so the two sides of the association stay in sync. */
    void addAccount(Account account) {
        if (account.getOwner() != this) {
            throw new IllegalArgumentException("Account belongs to a different customer");
        }
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Optional<Account> findAccount(String accountNumber) {
        return accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber)).findFirst();
    }

    public boolean hasAccountOfType(AccountType type) {
        return accounts.stream().anyMatch(a -> a.getAccountType() == type);
    }

    /** Total of all balances across every account the customer holds. */
    public BigDecimal getTotalBalance() {
        return accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmployed() {
        return employer != null;
    }

    // ---------- getters / validated setters ----------

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        this.firstName = requireText(firstName, "First name");
    }

    public String getSurname() {
        return surname;
    }

    public final void setSurname(String surname) {
        this.surname = requireText(surname, "Surname");
    }

    public String getNationalId() {
        return nationalId;
    }

    public final void setNationalId(String nationalId) {
        this.nationalId = requireText(nationalId, "National ID");
    }

    public String getAddress() {
        return address;
    }

    public final void setAddress(String address) {
        this.address = requireText(address, "Address");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + getUsername() + ")";
    }
}
