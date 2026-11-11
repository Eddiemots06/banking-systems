package com.bac.banking.exception;

import java.math.BigDecimal;

/** Thrown when a withdrawal would take an account balance below zero. */
public class InsufficientFundsException extends BankingException {
    private static final long serialVersionUID = 1L;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super("Insufficient funds: requested BWP " + requested + " but only BWP " + available + " is available.");
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
