package com.bac.banking.model;

import com.bac.banking.exception.AccountOpeningException;
import com.bac.banking.exception.InsufficientFundsException;
import com.bac.banking.exception.InvalidAmountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the core business rules of the three account types. */
class AccountModelTest {
    private Customer employed;
    private Customer unemployed;

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        employed = new Customer(1L, "Neo", "Seretse", "223456782", "Tlokweng", "72234562", "neo@mail.bw",
                "nseretse", "hash", "salt");
        employed.setEmployer(new Employer("Okavango Logistics", "Broadhurst, Gaborone", "Accountant"));
        unemployed = new Customer(2L, "Lesego", "Kgosi", "323456783", "Molepolole", null, null,
                "lkgosi", "hash", "salt");
    }

    @Test
    @DisplayName("Deposit increases the balance and records a transaction")
    void depositIncreasesBalance() throws Exception {
        SavingsAccount savings = new SavingsAccount("11000001", employed, "Main Mall");
        Transaction tx = savings.deposit(bd("250.50"));
        assertEquals(bd("250.50"), savings.getBalance());
        assertEquals(TransactionType.DEPOSIT, tx.getType());
        assertEquals(bd("250.50"), tx.getBalanceAfter());
        assertEquals(1, savings.getTransactions().size());
    }

    @Test
    @DisplayName("Overloaded deposit stores the reference")
    void overloadedDepositKeepsReference() throws Exception {
        SavingsAccount savings = new SavingsAccount("11000001", employed, "Main Mall");
        Transaction tx = savings.deposit(bd("100"), "Birthday money");
        assertEquals("Birthday money", tx.getReference());
    }

    @Test
    @DisplayName("Zero, negative and 3-decimal amounts are rejected")
    void invalidAmountsRejected() {
        SavingsAccount savings = new SavingsAccount("11000001", employed, "Main Mall");
        assertThrows(InvalidAmountException.class, () -> savings.deposit(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("-5")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("1.234")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(null));
        assertEquals(bd("0.00"), savings.getBalance());
    }

    @Test
    @DisplayName("A savings account cannot be withdrawn from")
    void savingsHasNoWithdraw() {
        Account savings = new SavingsAccount("11000001", employed, "Main Mall");
        assertFalse(savings instanceof Withdrawable);
        assertFalse(savings.canWithdraw());
        assertTrue(savings.earnsInterest());
    }

    @Test
    @DisplayName("Investment account needs at least BWP 500 to open")
    void investmentMinimumOpeningDeposit() throws Exception {
        assertThrows(AccountOpeningException.class,
                () -> new InvestmentAccount("21000001", employed, "Main Mall", bd("499.99")));
        assertEquals(0, employed.getAccounts().size(), "failed opening must not attach an account");

        InvestmentAccount inv = new InvestmentAccount("21000002", employed, "Main Mall", bd("500.00"));
        assertEquals(bd("500.00"), inv.getBalance());
        assertEquals(TransactionType.OPENING_DEPOSIT, inv.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Investment account allows withdrawals but not overdrafts")
    void investmentWithdrawals() throws Exception {
        InvestmentAccount inv = new InvestmentAccount("21000001", employed, "Main Mall", bd("1000"));
        inv.withdraw(bd("400"));
        assertEquals(bd("600.00"), inv.getBalance());
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> inv.withdraw(bd("600.01")));
        assertEquals(bd("600.00"), ex.getAvailable());
        assertEquals(bd("600.00"), inv.getBalance());
    }

    @Test
    @DisplayName("Cheque account can only be opened for an employed customer")
    void chequeRequiresEmployment() throws Exception {
        assertThrows(AccountOpeningException.class, () -> new ChequeAccount("31000001", unemployed, "Maun"));
        ChequeAccount cheque = new ChequeAccount("31000002", employed, "Main Mall");
        assertEquals("Okavango Logistics", cheque.getEmployer().getCompanyName());
        assertFalse(cheque.earnsInterest());
    }

    @Test
    @DisplayName("Cheque account takes salary and allows withdrawals of any amount up to the balance")
    void chequeSalaryAndWithdraw() throws Exception {
        ChequeAccount cheque = new ChequeAccount("31000001", employed, "Main Mall");
        Transaction salary = cheque.depositSalary(bd("8000"));
        assertEquals("Salary - Okavango Logistics", salary.getReference());
        cheque.withdraw(bd("8000"));
        assertEquals(bd("0.00"), cheque.getBalance());
        assertThrows(InsufficientFundsException.class, () -> cheque.withdraw(bd("0.01")));
    }

    @Test
    @DisplayName("An account cannot exist without a customer")
    void accountNeedsCustomer() {
        assertThrows(NullPointerException.class, () -> new SavingsAccount("11000001", null, "Main Mall"));
    }

    @Test
    @DisplayName("A customer can hold all three account types at once")
    void customerHoldsMultipleAccounts() throws Exception {
        new SavingsAccount("11000001", employed, "Main Mall").deposit(bd("100"));
        new InvestmentAccount("21000001", employed, "Main Mall", bd("500"));
        new ChequeAccount("31000001", employed, "Main Mall").deposit(bd("50"));
        assertEquals(3, employed.getAccounts().size());
        assertTrue(employed.hasAccountOfType(AccountType.SAVINGS));
        assertTrue(employed.hasAccountOfType(AccountType.INVESTMENT));
        assertTrue(employed.hasAccountOfType(AccountType.CHEQUE));
        assertEquals(bd("650.00"), employed.getTotalBalance());
    }
}
