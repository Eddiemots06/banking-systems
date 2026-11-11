package com.bac.banking.model;

import com.bac.banking.exception.AccountOpeningException;
import com.bac.banking.exception.InsufficientFundsException;
import com.bac.banking.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cheque account: used for salary payments. Deposits and withdrawals of any amount
 * are allowed, no interest is paid, and it can only be opened for an employed customer.
 */
public class ChequeAccount extends Account implements Withdrawable {

    /** Opens a new cheque account; the customer must have employer details. */
    public ChequeAccount(String accountNumber, Customer owner, String branch) throws AccountOpeningException {
        super(accountNumber, requireEmployed(owner), branch);
    }

    /** Rebuilds an existing cheque account from the database. */
    public ChequeAccount(String accountNumber, Customer owner, String branch, BigDecimal balance, LocalDate dateOpened) {
        super(accountNumber, owner, branch, balance, dateOpened);
    }

    private static Customer requireEmployed(Customer owner) throws AccountOpeningException {
        if (owner == null || !owner.isEmployed()) {
            throw new AccountOpeningException(
                    "A Cheque Account can only be opened for a working customer. Please add employer details first.");
        }
        return owner;
    }

    /** The employer this account receives salary from (read from the owner). */
    public Employer getEmployer() {
        return getOwner().getEmployer();
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CHEQUE;
    }

    @Override
    public String getAccountRules() {
        return "Salary account. Deposits and withdrawals of any amount. No interest. Customer must be employed.";
    }

    @Override
    public Transaction withdraw(BigDecimal amount) throws InvalidAmountException, InsufficientFundsException {
        return withdraw(amount, "Withdrawal");
    }

    @Override
    public Transaction withdraw(BigDecimal amount, String reference)
            throws InvalidAmountException, InsufficientFundsException {
        BigDecimal value = validateAmount(amount);
        if (value.compareTo(getBalance()) > 0) {
            throw new InsufficientFundsException(getBalance(), value);
        }
        return debit(value, TransactionType.WITHDRAWAL, reference);
    }

    /** Convenience overload for salary credits: records the employer as the reference. */
    public Transaction depositSalary(BigDecimal amount) throws InvalidAmountException {
        return deposit(amount, "Salary - " + getEmployer().getCompanyName());
    }
}
