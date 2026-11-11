package com.bac.banking.model;

import com.bac.banking.exception.InvalidAmountException;

import java.math.BigDecimal;

/**
 * Capability interface: an object that can receive deposits.
 * Every account type implements this through the abstract {@link Account} class.
 */
public interface Depositable {

    Transaction deposit(BigDecimal amount) throws InvalidAmountException;

    /** Overloaded version that also stores a reference/description for the deposit. */
    Transaction deposit(BigDecimal amount, String reference) throws InvalidAmountException;
}
