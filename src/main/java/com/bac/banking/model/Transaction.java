package com.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An immutable record of one movement of money on an account.
 * The id is null until the transaction has been saved to the database.
 */
public class Transaction {
    private Long transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String reference;

    /** Constructor for a brand-new transaction created by the domain model. */
    public Transaction(String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String reference) {
        this(null, accountNumber, type, amount, balanceAfter, LocalDateTime.now(), reference);
    }

    /** Overloaded constructor used when re-building a saved transaction from the database. */
    public Transaction(Long transactionId, String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, LocalDateTime timestamp, String reference) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.reference = reference;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    /** Set once by the DAO after the database generates the key. */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return timestamp + " " + type + " BWP " + amount + " on " + accountNumber + " (balance BWP " + balanceAfter + ")";
    }
}
