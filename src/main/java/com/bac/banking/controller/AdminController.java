package com.bac.banking.controller;

import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.controller.dto.InterestRunResult;
import com.bac.banking.dao.AccountDAO;
import com.bac.banking.exception.DataAccessException;
import com.bac.banking.exception.ValidationException;
import com.bac.banking.model.Account;
import com.bac.banking.model.Bank;
import com.bac.banking.model.InterestPayment;
import com.bac.banking.util.MoneyFormat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the administrator's use cases: view all accounts and Pay Monthly Interest.
 * Interest is paid polymorphically - the Bank asks each InterestBearing account for its own rate.
 */
public class AdminController {
    private final AccountDAO accountDAO;
    private final Bank bank;
    private final Session session;

    public AdminController(AccountDAO accountDAO, Bank bank, Session session) {
        this.accountDAO = accountDAO;
        this.bank = bank;
        this.session = session;
    }

    public List<AccountSummary> getAllAccounts() throws ValidationException {
        requireAdmin();
        return accountDAO.findAll().stream().map(AccountController::toSummary).toList();
    }

    /**
     * Pays one month's interest to every Savings (0.05%) and Investment (5%) account.
     * Each payment moves through the InterestPayment states; a failure on one account
     * does not stop the others.
     */
    public InterestRunResult payMonthlyInterest() throws ValidationException {
        requireAdmin();
        List<Account> accounts = accountDAO.findAll();
        List<InterestPayment> payments = bank.prepareMonthlyInterest(accounts);

        int paid = 0;
        int skipped = 0;
        int failed = 0;
        BigDecimal total = BigDecimal.ZERO;
        List<String> details = new ArrayList<>();

        for (InterestPayment payment : payments) {
            Account account = (Account) payment.getAccount();
            String label = account.getAccountNumber() + " (" + account.getAccountType().getDisplayName() + ")";
            if (payment.getStatus() == InterestPayment.Status.SKIPPED) {
                skipped++;
                details.add(label + ": skipped - no interest due on a zero balance");
                continue;
            }
            try {
                payment.credit();
                accountDAO.applyTransaction(account, payment.getTransaction());
                payment.markRecorded();
                paid++;
                total = total.add(payment.getAmount());
                details.add(label + ": paid " + MoneyFormat.bwp(payment.getAmount())
                        + ", new balance " + MoneyFormat.bwp(account.getBalance()));
            } catch (DataAccessException | IllegalStateException e) {
                payment.fail(e.getMessage());
                failed++;
                details.add(label + ": FAILED - " + e.getMessage());
            }
        }
        return new InterestRunResult(paid, skipped, failed, MoneyFormat.bwp(total), details);
    }

    public String getBankName() {
        return bank.getName();
    }

    private void requireAdmin() throws ValidationException {
        if (!session.isAdministrator()) {
            throw new ValidationException("Only an administrator can do this.");
        }
    }
}
