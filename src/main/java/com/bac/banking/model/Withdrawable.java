package com.bac.banking.model;

import com.bac.banking.exception.InsufficientFundsException;
import com.bac.banking.exception.InvalidAmountException;

import java.math.BigDecimal;

/**
 * Capability interface: an account that allows withdrawals.
 * Implemented by InvestmentAccount and ChequeAccount, but NOT SavingsAccount,
 * so the compiler itself prevents a withdrawal from a savings account.
 */
public interface Withdrawable {

    Transaction withdraw(BigDecimal amount) throws InvalidAmountException, InsufficientFundsException;

    /** Overloaded version that also stores a reference/description for the withdrawal. */
    Transaction withdraw(BigDecimal amount, String reference)
            throws InvalidAmountException, InsufficientFundsException;
}
