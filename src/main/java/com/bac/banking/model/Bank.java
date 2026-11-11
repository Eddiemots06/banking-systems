package com.bac.banking.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The bank itself. Demonstrates polymorphism: it works with a list of the abstract
 * type {@link Account} and only pays interest to objects that are {@link InterestBearing},
 * letting each subclass supply its own rate.
 */
public class Bank {
    private final String name;
    private final String swiftCode;

    public Bank(String name, String swiftCode) {
        this.name = name;
        this.swiftCode = swiftCode;
    }

    /**
     * Creates an InterestPayment for every interest-bearing account and moves it to
     * CALCULATED (or SKIPPED). Cheque accounts are ignored because they are not InterestBearing.
     */
    public List<InterestPayment> prepareMonthlyInterest(List<? extends Account> accounts) {
        List<InterestPayment> payments = new ArrayList<>();
        for (Account account : accounts) {
            if (account instanceof InterestBearing interestBearing) {
                InterestPayment payment = new InterestPayment(interestBearing);
                payment.calculate();
                payments.add(payment);
            }
        }
        return payments;
    }

    public String getName() {
        return name;
    }

    public String getSwiftCode() {
        return swiftCode;
    }
}
