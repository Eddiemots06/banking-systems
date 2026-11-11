package com.bac.banking.model;

import com.bac.banking.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for every bank account.
 *
 * <p>An account cannot exist without a customer: the owner is a required constructor
 * argument and the constructor registers the account with that customer (composition).
 * All fields are private and only change through validated methods (encapsulation).</p>
 *
 * <p>Every account accepts deposits, so the deposit behaviour lives here. Withdrawals and
 * interest are optional capabilities added by the {@link Withdrawable} and
 * {@link InterestBearing} interfaces in the subclasses.</p>
 */
public abstract class Account implements Depositable {
    private final String accountNumber;
    private final Customer owner;
    private String branch;
    private BigDecimal balance;
    private final LocalDate dateOpened;
    private final List<Transaction> transactions = new ArrayList<>();

    /** Constructor for opening a new account today with a zero balance. */
    protected Account(String accountNumber, Customer owner, String branch) {
        this(accountNumber, owner, branch, BigDecimal.ZERO, LocalDate.now());
    }

    /** Overloaded constructor used to rebuild an existing account loaded from the database. */
    protected Account(String accountNumber, Customer owner, String branch, BigDecimal balance, LocalDate dateOpened) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        this.owner = Objects.requireNonNull(owner, "An account cannot exist without a customer");
        this.accountNumber = accountNumber;
        setBranch(branch);
        this.balance = money(balance == null ? BigDecimal.ZERO : balance);
        this.dateOpened = dateOpened == null ? LocalDate.now() : dateOpened;
        owner.addAccount(this);
    }

    // ---------- abstract members each subclass must provide ----------

    /** Which of the three account types this object is. */
    public abstract AccountType getAccountType();

    /** Short human-readable rules for this account, shown in the GUI. */
    public abstract String getAccountRules();

    // ---------- deposits (shared by all accounts) ----------

    @Override
    public Transaction deposit(BigDecimal amount) throws InvalidAmountException {
        return deposit(amount, "Deposit");
    }

    @Override
    public Transaction deposit(BigDecimal amount, String reference) throws InvalidAmountException {
        BigDecimal value = validateAmount(amount);
        return credit(value, TransactionType.DEPOSIT, reference);
    }

    // ---------- helpers for subclasses ----------

    /** Adds money and records a transaction. */
    protected Transaction credit(BigDecimal amount, TransactionType type, String reference) {
        balance = money(balance.add(amount));
        return record(type, amount, reference);
    }

    /** Removes money and records a transaction. The caller must check funds first. */
    protected Transaction debit(BigDecimal amount, TransactionType type, String reference) {
        balance = money(balance.subtract(amount));
        return record(type, amount, reference);
    }

    private Transaction record(TransactionType type, BigDecimal amount, String reference) {
        Transaction tx = new Transaction(accountNumber, type, money(amount), balance,
                reference == null || reference.isBlank() ? type.name() : reference.trim());
        transactions.add(tx);
        return tx;
    }

    /** Rejects null, zero and negative amounts and amounts with more than 2 decimal places. */
    protected static BigDecimal validateAmount(BigDecimal amount) throws InvalidAmountException {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount cannot have more than 2 decimal places.");
        }
        return money(amount);
    }

    protected static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    // ---------- getters / setters ----------

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getOwner() {
        return owner;
    }

    public String getBranch() {
        return branch;
    }

    public final void setBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required");
        }
        this.branch = branch.trim();
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    /** Transactions made on this object since it was created or loaded (read-only view). */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public boolean canWithdraw() {
        return this instanceof Withdrawable;
    }

    public boolean earnsInterest() {
        return this instanceof InterestBearing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    @Override
    public String toString() {
        return getAccountType().getDisplayName() + " " + accountNumber + " (BWP " + balance + ")";
    }
}
