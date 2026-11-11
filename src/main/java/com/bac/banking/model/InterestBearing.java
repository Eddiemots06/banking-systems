package com.bac.banking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Capability interface: an account that earns monthly interest.
 * Implemented by SavingsAccount (0.05% per month) and InvestmentAccount (5% per month).
 */
public interface InterestBearing {

    /** Monthly rate as a fraction, e.g. 0.05 for 5%. Each implementing class overrides this. */
    BigDecimal getMonthlyInterestRate();

    /** The current balance the interest is calculated on. */
    BigDecimal getBalance();

    /** Calculates (but does not pay) this month's interest, rounded to the nearest thebe. */
    default BigDecimal calculateMonthlyInterest() {
        return getBalance().multiply(getMonthlyInterestRate()).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Credits an already calculated amount of interest to the account. */
    Transaction creditInterest(BigDecimal interest);
}
