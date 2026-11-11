package com.bac.banking.exception;

/**
 * Base checked exception for every business-rule violation in the banking domain.
 * Controllers catch this type and turn the message into feedback for the user.
 */
public class BankingException extends Exception {
    private static final long serialVersionUID = 1L;
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
