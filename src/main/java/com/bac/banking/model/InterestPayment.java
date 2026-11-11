package com.bac.banking.model;

import java.math.BigDecimal;

/**
 * Tracks the monthly interest payment for ONE interest-bearing account through its lifecycle.
 * This is the class modelled by the State Diagram:
 *
 * <pre>
 *  PENDING --calculate()--> CALCULATED --credit()--> CREDITED --markRecorded()--> RECORDED
 *     |                       |                        |
 *     |  (interest == 0)      |                        |
 *     +--calculate()--> SKIPPED                         |
 *     +--------------------fail(reason)--------------> FAILED
 * </pre>
 */
public class InterestPayment {

    public enum Status { PENDING, CALCULATED, SKIPPED, CREDITED, RECORDED, FAILED }

    private final InterestBearing account;
    private Status status = Status.PENDING;
    private BigDecimal amount = BigDecimal.ZERO;
    private Transaction transaction;
    private String failureReason;

    public InterestPayment(InterestBearing account) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }
        this.account = account;
    }

    /** PENDING -> CALCULATED, or PENDING -> SKIPPED when the interest rounds to zero. */
    public void calculate() {
        requireStatus(Status.PENDING);
        amount = account.calculateMonthlyInterest();
        status = amount.signum() > 0 ? Status.CALCULATED : Status.SKIPPED;
    }

    /** CALCULATED -> CREDITED: the interest is added to the account balance. */
    public Transaction credit() {
        requireStatus(Status.CALCULATED);
        transaction = account.creditInterest(amount);
        status = Status.CREDITED;
        return transaction;
    }

    /** CREDITED -> RECORDED: the new balance and transaction have been saved. */
    public void markRecorded() {
        requireStatus(Status.CREDITED);
        status = Status.RECORDED;
    }

    /** Any non-final state -> FAILED. */
    public void fail(String reason) {
        if (isFinished()) {
            throw new IllegalStateException("Cannot fail a payment that is already " + status);
        }
        failureReason = reason;
        status = Status.FAILED;
    }

    public boolean isFinished() {
        return status == Status.RECORDED || status == Status.SKIPPED || status == Status.FAILED;
    }

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + " but was " + status);
        }
    }

    public InterestBearing getAccount() {
        return account;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
