package com.bac.banking.controller;

import com.bac.banking.controller.dto.AccountSummary;
import com.bac.banking.controller.dto.TransactionRow;
import com.bac.banking.dao.AccountDAO;
import com.bac.banking.dao.CustomerDAO;
import com.bac.banking.dao.TransactionDAO;
import com.bac.banking.exception.BankingException;
import com.bac.banking.exception.DataAccessException;
import com.bac.banking.exception.ValidationException;
import com.bac.banking.model.Account;
import com.bac.banking.model.AccountType;
import com.bac.banking.model.ChequeAccount;
import com.bac.banking.model.Customer;
import com.bac.banking.model.InvestmentAccount;
import com.bac.banking.model.SavingsAccount;
import com.bac.banking.model.Transaction;
import com.bac.banking.model.Withdrawable;
import com.bac.banking.util.MoneyFormat;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the account use cases for a logged-in customer: view accounts, open an account,
 * deposit, withdraw and view a statement. It mediates between the views and the domain model,
 * and asks the DAOs to save every change.
 */
public class AccountController {
    public static final List<String> BRANCHES = List.of(
            "Main Mall", "Game City", "Fairgrounds", "Francistown", "Maun", "Molepolole",
            "Palapye", "Serowe", "Kasane", "Lobatse");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final Session session;

    public AccountController(CustomerDAO customerDAO, AccountDAO accountDAO, TransactionDAO transactionDAO,
                             Session session) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.session = session;
    }

    // ---------- queries ----------

    public List<AccountSummary> getMyAccounts() throws ValidationException {
        Customer customer = loadCustomer();
        return customer.getAccounts().stream().map(AccountController::toSummary).toList();
    }

    public String getTotalBalance() throws ValidationException {
        return MoneyFormat.bwp(loadCustomer().getTotalBalance());
    }

    public List<String> getAccountTypes() {
        return Arrays.stream(AccountType.values()).map(AccountType::getDisplayName).toList();
    }

    public List<String> getBranches() {
        return BRANCHES;
    }

    /** Rules text for the account type picked in the Open Account form. */
    public String describeAccountType(String displayName) {
        return switch (parseType(displayName)) {
            case SAVINGS -> "Deposits only - no withdrawals. Earns 0.05% interest per month.";
            case INVESTMENT -> "Deposits and withdrawals. Earns 5% interest per month. "
                    + "Minimum opening deposit BWP 500.00.";
            case CHEQUE -> "Salary account. Deposits and withdrawals of any amount. No interest. "
                    + "Only for employed customers.";
        };
    }

    public List<TransactionRow> getStatement(String accountNumber) throws ValidationException {
        ownedAccount(loadCustomer(), accountNumber);
        return transactionDAO.findByAccount(accountNumber).stream()
                .map(t -> new TransactionRow(
                        t.getTimestamp().format(DATE_TIME),
                        pretty(t.getType().name()),
                        MoneyFormat.bwp(t.getAmount()),
                        MoneyFormat.bwp(t.getBalanceAfter()),
                        t.getReference()))
                .toList();
    }

    // ---------- commands ----------

    /** Use case: Open Account. Returns the new account. */
    public AccountSummary openAccount(String accountTypeName, String branch, String openingDepositText)
            throws BankingException {
        Customer customer = loadCustomer();
        AccountType type = parseType(accountTypeName);
        String chosenBranch = InputValidator.required(branch, "Branch");
        BigDecimal opening = InputValidator.openingAmount(openingDepositText);

        if (type == AccountType.CHEQUE && !customer.isEmployed()) {
            throw new ValidationException("A Cheque Account is only for working customers. "
                    + "Add your employer details (Update Employment) first.");
        }
        if (type == AccountType.INVESTMENT && opening.compareTo(InvestmentAccount.MINIMUM_OPENING_DEPOSIT) < 0) {
            throw new ValidationException("An Investment Account needs an opening deposit of at least BWP 500.00.");
        }

        String number = accountDAO.nextAccountNumber(type);
        Account account = switch (type) {
            case SAVINGS -> new SavingsAccount(number, customer, chosenBranch);
            case INVESTMENT -> new InvestmentAccount(number, customer, chosenBranch, opening);
            case CHEQUE -> new ChequeAccount(number, customer, chosenBranch);
        };
        if (type != AccountType.INVESTMENT && opening.signum() > 0) {
            account.deposit(opening, "Opening deposit");
        }
        accountDAO.create(account);
        return toSummary(account);
    }

    /** Use case: Deposit Funds. Works on any account type (polymorphism through Account/Depositable). */
    public AccountSummary deposit(String accountNumber, String amountText, String reference) throws BankingException {
        BigDecimal amount = InputValidator.amount(amountText);
        Account account = ownedAccount(loadCustomer(), accountNumber);
        Transaction tx = account.deposit(amount, referenceOr(reference, "Deposit"));
        save(account, tx);
        return toSummary(account);
    }

    /** Use case: Withdraw Funds. Only accounts implementing Withdrawable are allowed. */
    public AccountSummary withdraw(String accountNumber, String amountText, String reference) throws BankingException {
        BigDecimal amount = InputValidator.amount(amountText);
        Account account = ownedAccount(loadCustomer(), accountNumber);
        if (!(account instanceof Withdrawable withdrawable)) {
            throw new ValidationException("Withdrawals are not allowed from a "
                    + account.getAccountType().getDisplayName() + ".");
        }
        Transaction tx = withdrawable.withdraw(amount, referenceOr(reference, "Withdrawal"));
        save(account, tx);
        return toSummary(account);
    }

    // ---------- helpers ----------

    private void save(Account account, Transaction tx) throws BankingException {
        try {
            accountDAO.applyTransaction(account, tx);
        } catch (DataAccessException e) {
            // Nothing was committed, and the in-memory object is discarded on the next request.
            throw new BankingException("The transaction could not be saved. No money was moved. Please try again.", e);
        }
    }

    /** Always works on fresh data from the database. */
    private Customer loadCustomer() throws ValidationException {
        if (!session.isCustomer()) {
            throw new ValidationException("Please log in as a customer first.");
        }
        Customer customer = customerDAO.findById(session.getUserId())
                .orElseThrow(() -> new ValidationException("Your customer record could not be found."));
        accountDAO.findByCustomer(customer);
        return customer;
    }

    /** A customer may only act on their own accounts. */
    private Account ownedAccount(Customer customer, String accountNumber) throws ValidationException {
        String number = InputValidator.required(accountNumber, "Account");
        return customer.findAccount(number)
                .orElseThrow(() -> new ValidationException("Account " + number + " does not belong to you."));
    }

    private static AccountType parseType(String displayName) {
        for (AccountType type : AccountType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName) || type.name().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + displayName);
    }

    private static String referenceOr(String reference, String fallback) {
        return reference == null || reference.isBlank() ? fallback : reference.trim();
    }

    static String pretty(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    static AccountSummary toSummary(Account a) {
        return new AccountSummary(
                a.getAccountNumber(),
                a.getAccountType().getDisplayName(),
                a.getOwner().getDisplayName(),
                a.getBranch(),
                MoneyFormat.bwp(a.getBalance()),
                a.getDateOpened().format(DATE),
                a.canWithdraw(),
                a.earnsInterest(),
                a.getAccountRules());
    }
}
