package com.bac.banking.exception;

/** Thrown when an amount is zero, negative or otherwise not acceptable. */
public class InvalidAmountException extends BankingException {
    private static final long serialVersionUID = 1L;
    public InvalidAmountException(String message) {
        super(message);
    }
}
