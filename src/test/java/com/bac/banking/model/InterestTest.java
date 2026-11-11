package com.bac.banking.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for interest calculation, polymorphism and the InterestPayment state machine. */
class InterestTest {
    private Customer customer;

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        customer = new Customer(1L, "Kabelo", "Masire", "113456780", "Gaborone", null, null,
                "kmasire", "hash", "salt");
        customer.setEmployer(new Employer("Sedibeng Software", "CBD, Gaborone", "Developer"));
    }

    @Test
    @DisplayName("Savings earns 0.05% per month")
    void savingsInterest() throws Exception {
        SavingsAccount savings = new SavingsAccount("11000001", customer, "Main Mall");
        savings.deposit(bd("10000"));
        assertEquals(bd("5.00"), savings.calculateMonthlyInterest());
    }

    @Test
    @DisplayName("Investment earns 5% per month")
    void investmentInterest() throws Exception {
        InvestmentAccount inv = new InvestmentAccount("21000001", customer, "Main Mall", bd("1000"));
        assertEquals(bd("50.00"), inv.calculateMonthlyInterest());
    }

    @Test
    @DisplayName("Bank pays interest polymorphically and ignores cheque accounts")
    void bankPaysPolymorphically() throws Exception {
        SavingsAccount savings = new SavingsAccount("11000001", customer, "Main Mall");
        savings.deposit(bd("2000"));
        InvestmentAccount inv = new InvestmentAccount("21000001", customer, "Main Mall", bd("1000"));
        ChequeAccount cheque = new ChequeAccount("31000001", customer, "Main Mall");
        cheque.deposit(bd("5000"));

        List<InterestPayment> payments = new Bank("BAC Bank", "BACBBWGX")
                .prepareMonthlyInterest(List.of(savings, inv, cheque));
        assertEquals(2, payments.size(), "cheque account is not InterestBearing");
        payments.forEach(InterestPayment::credit);

        assertEquals(bd("2001.00"), savings.getBalance());
        assertEquals(bd("1050.00"), inv.getBalance());
        assertEquals(bd("5000.00"), cheque.getBalance());
    }

    @Test
    @DisplayName("InterestPayment moves PENDING -> CALCULATED -> CREDITED -> RECORDED")
    void interestPaymentHappyPath() throws Exception {
        InvestmentAccount inv = new InvestmentAccount("21000001", customer, "Main Mall", bd("1000"));
        InterestPayment payment = new InterestPayment(inv);
        assertEquals(InterestPayment.Status.PENDING, payment.getStatus());
        payment.calculate();
        assertEquals(InterestPayment.Status.CALCULATED, payment.getStatus());
        payment.credit();
        assertEquals(InterestPayment.Status.CREDITED, payment.getStatus());
        assertEquals(TransactionType.INTEREST, payment.getTransaction().getType());
        payment.markRecorded();
        assertEquals(InterestPayment.Status.RECORDED, payment.getStatus());
        assertTrue(payment.isFinished());
    }

    @Test
    @DisplayName("Zero balance is SKIPPED and invalid transitions are refused")
    void interestPaymentSkippedAndGuards() {
        SavingsAccount empty = new SavingsAccount("11000001", customer, "Main Mall");
        InterestPayment payment = new InterestPayment(empty);
        assertThrows(IllegalStateException.class, payment::credit);
        payment.calculate();
        assertEquals(InterestPayment.Status.SKIPPED, payment.getStatus());
        assertThrows(IllegalStateException.class, payment::credit);
        assertThrows(IllegalStateException.class, () -> payment.fail("too late"));
    }

    @Test
    @DisplayName("A payment can fail before it is recorded")
    void interestPaymentFails() throws Exception {
        InvestmentAccount inv = new InvestmentAccount("21000001", customer, "Main Mall", bd("1000"));
        InterestPayment payment = new InterestPayment(inv);
        payment.calculate();
        payment.credit();
        payment.fail("database offline");
        assertEquals(InterestPayment.Status.FAILED, payment.getStatus());
        assertEquals("database offline", payment.getFailureReason());
    }
}
