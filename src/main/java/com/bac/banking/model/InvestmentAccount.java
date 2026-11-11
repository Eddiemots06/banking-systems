package com.bac.banking.model;

import com.bac.banking.exception.AccountOpeningException;
import com.bac.banking.exception.InsufficientFundsException;
import com.bac.banking.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Investment account: deposits and withdrawals allowed, earns 5% interest per month,
 * and can only be opened with an initial deposit of at least BWP 500.00.
 */
public class InvestmentAccount extends Account implements InterestBearing, Withdrawable {
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.05");
    public static final BigDecimal MINIMUM_OPENING_DEPOSIT = new BigDecimal("500.00");

    /** Opens a new investment account; enforces the BWP 500 minimum opening deposit. */
    public InvestmentAccount(String accountNumber, Customer owner, String branch, BigDecimal openingDeposit)
            throws AccountOpeningException {
        super(accountNumber, validateOpening(owner, openingDeposit), branch);
        credit(money(openingDeposit), TransactionType.OPENING_DEPOSIT, "Opening deposit");
    }

    /** Rebuilds an existing investment account from the database. */
    public InvestmentAccount(String accountNumber, Customer owner, String branch, BigDecimal balance, LocalDate dateOpened) {
        super(accountNumber, owner, branch, balance, dateOpened);
    }

    /** Runs before the account is attached to its owner, so a failed opening leaves no trace. */
    private static Customer validateOpening(Customer owner, BigDecimal openingDeposit) throws AccountOpeningException {
        if (openingDeposit == null || openingDeposit.compareTo(MINIMUM_OPENING_DEPOSIT) < 0) {
            throw new AccountOpeningException("An Investment Account requires an opening deposit of at least BWP "
                    + MINIMUM_OPENING_DEPOSIT + ".");
        }
        return owner;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.INVESTMENT;
    }

    @Override
    public String getAccountRules() {
        return "Deposits and withdrawals allowed. Earns 5% interest per month. Minimum opening deposit BWP 500.00.";
    }

    @Override
    public BigDecimal getMonthlyInterestRate() {
        return MONTHLY_RATE;
    }

    @Override
    public Transaction creditInterest(BigDecimal interest) {
        return credit(money(interest), TransactionType.INTEREST, "Monthly interest 5%");
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
}
