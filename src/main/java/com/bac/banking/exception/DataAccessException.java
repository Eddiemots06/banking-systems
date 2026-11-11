package com.bac.banking.exception;

/**
 * Unchecked wrapper for SQLExceptions so that JDBC details do not leak
 * out of the data access layer into controllers or the model.
 */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
