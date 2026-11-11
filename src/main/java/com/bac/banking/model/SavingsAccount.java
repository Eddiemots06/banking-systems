package com.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Savings account: deposits only, no withdrawals, earns 0.05% interest per month.
 * It deliberately does NOT implement {@link Withdrawable}.
 */
public class SavingsAccount extends Account implements InterestBearing {
    /** 0.05% per month expressed as a fraction. */
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0005");

    public SavingsAccount(String accountNumber, Customer owner, String branch) {
        super(accountNumber, owner, branch);
    }

    public SavingsAccount(String accountNumber, Customer owner, String branch, BigDecimal balance, LocalDate dateOpened) {
        super(accountNumber, owner, branch, balance, dateOpened);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public String getAccountRules() {
        return "Deposits only - withdrawals are not allowed. Earns 0.05% interest per month.";
    }

    @Override
    public BigDecimal getMonthlyInterestRate() {
        return MONTHLY_RATE;
    }

    @Override
    public Transaction creditInterest(BigDecimal interest) {
        return credit(money(interest), TransactionType.INTEREST, "Monthly interest 0.05%");
    }
}
