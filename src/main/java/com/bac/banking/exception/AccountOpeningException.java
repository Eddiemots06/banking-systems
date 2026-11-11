package com.bac.banking.exception;

/** Thrown when an account cannot be opened because an opening rule is not met. */
public class AccountOpeningException extends BankingException {
    private static final long serialVersionUID = 1L;
    public AccountOpeningException(String message) {
        super(message);
    }
}
