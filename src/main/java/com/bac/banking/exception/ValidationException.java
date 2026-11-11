package com.bac.banking.exception;

/** Thrown by controllers when user input fails validation (empty fields, bad formats). */
public class ValidationException extends BankingException {
    private static final long serialVersionUID = 1L;
    public ValidationException(String message) {
        super(message);
    }
}
