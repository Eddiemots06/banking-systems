package com.bac.banking.dao;

import com.bac.banking.exception.BankingException;
import com.bac.banking.model.Account;
import com.bac.banking.model.AccountType;
import com.bac.banking.model.Administrator;
import com.bac.banking.model.ChequeAccount;
import com.bac.banking.model.Customer;
import com.bac.banking.model.Employer;
import com.bac.banking.model.InvestmentAccount;
import com.bac.banking.model.SavingsAccount;
import com.bac.banking.model.Transaction;
import com.bac.banking.model.Withdrawable;
import com.bac.banking.util.PasswordUtil;

import java.math.BigDecimal;

/**
 * Inserts sample data the first time the application runs on an empty database:
 * 10 customers, 16 accounts and their transactions, plus one administrator.
 *
 * <p>All customer passwords are {@value #DEMO_PASSWORD}; the admin logs in with
 * {@value #ADMIN_USERNAME} / {@value #ADMIN_PASSWORD}.</p>
 */
public class DataSeeder {
    public static final String DEMO_PASSWORD = "Password@123";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "Admin@123";

    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final AdministratorDAO administratorDAO;

    public DataSeeder(CustomerDAO customerDAO, AccountDAO accountDAO, AdministratorDAO administratorDAO) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
        this.administratorDAO = administratorDAO;
    }

    /** Seeds only when there are no customers yet. Returns true if data was inserted. */
    public boolean seedIfEmpty() {
        if (customerDAO.count() > 0) {
            return false;
        }
        try {
            seed();
            return true;
        } catch (BankingException e) {
            throw new IllegalStateException("Sample data broke a business rule: " + e.getMessage(), e);
        }
    }

    private void seed() throws BankingException {
        String adminSalt = PasswordUtil.generateSalt();
        administratorDAO.create(new Administrator(null, ADMIN_USERNAME, "Bank Administrator",
                PasswordUtil.hash(ADMIN_PASSWORD, adminSalt), adminSalt));

        Customer kagiso = customer("Kagiso", "Molefe", "123456781", "Plot 1234, Block 6, Gaborone",
                "71234561", "kagiso.molefe@mail.bw", "kmolefe",
                new Employer("Kalahari Mining Ltd", "Plot 50, Main Mall, Gaborone", "Mining Engineer"));
        Customer neo = customer("Neo", "Seretse", "223456782", "Plot 77, Tlokweng",
                "72234562", "neo.seretse@mail.bw", "nseretse",
                new Employer("Okavango Logistics", "Plot 9, Broadhurst Industrial, Gaborone", "Accountant"));
        Customer lesego = customer("Lesego", "Kgosi", "323456783", "Ward 4, Molepolole",
                "73234563", "lesego.kgosi@mail.bw", "lkgosi", null);
        Customer thato = customer("Thato", "Mokoena", "423456784", "Plot 812, Francistown",
                "74234564", "thato.mokoena@mail.bw", "tmokoena",
                new Employer("Francistown City Council", "Blue Jacket Street, Francistown", "Town Planner"));
        Customer boitumelo = customer("Boitumelo", "Dikgang", "523456785", "Plot 3301, Maun",
                "75234565", "boitumelo.d@mail.bw", "bdikgang",
                new Employer("Delta Safaris (Pty) Ltd", "Mathiba Road, Maun", "Tour Guide"));
        Customer mpho = customer("Mpho", "Tau", "623456786", "Plot 45, Palapye",
                "76234566", "mpho.tau@mail.bw", "mtau", null);
        Customer onalenna = customer("Onalenna", "Phiri", "723456787", "Plot 1900, Lobatse",
                "77234567", "onalenna.phiri@mail.bw", "ophiri",
                new Employer("Botswana Accountancy College", "Plot 50661, Fairgrounds, Gaborone", "Lecturer"));
        Customer tebogo = customer("Tebogo", "Ramotswe", "823456788", "Plot 600, Serowe",
                "78234568", "tebogo.r@mail.bw", "tramotswe", null);
        Customer naledi = customer("Naledi", "Moremi", "923456789", "Plot 22, Kasane",
                "71334569", "naledi.moremi@mail.bw", "nmoremi",
                new Employer("Chobe Water Utilities", "President Avenue, Kasane", "Technician"));
        Customer kabelo = customer("Kabelo", "Masire", "113456780", "Plot 5050, Phase 2, Gaborone",
                "72334560", "kabelo.masire@mail.bw", "kmasire",
                new Employer("Sedibeng Software", "iTowers, CBD, Gaborone", "Software Developer"));

        // Kagiso: savings + cheque
        savings(kagiso, "Main Mall", "2500.00");
        cheque(kagiso, "Main Mall", "8000.00", "1200.00");
        // Neo: all three account types
        investment(neo, "Game City", "15000.00");
        savings(neo, "Game City", "1200.00");
        cheque(neo, "Game City", "6500.00", "750.00");
        // Lesego: student, savings only
        savings(lesego, "Molepolole", "300.00");
        // Thato: cheque + investment
        cheque(thato, "Francistown", "12000.00", "3000.00");
        investment(thato, "Francistown", "5000.00");
        // Boitumelo: cheque
        cheque(boitumelo, "Maun", "4300.00", "500.00");
        // Mpho: investment at the minimum
        investment(mpho, "Palapye", "500.00");
        // Onalenna: savings + cheque
        savings(onalenna, "Fairgrounds", "10000.00");
        cheque(onalenna, "Fairgrounds", "18500.00", "2500.00");
        // Tebogo: savings
        savings(tebogo, "Serowe", "750.00");
        // Naledi: investment
        investment(naledi, "Kasane", "2000.00");
        // Kabelo: investment + cheque
        investment(kabelo, "Main Mall", "25000.00");
        cheque(kabelo, "Main Mall", "22000.00", "4000.00");
    }

    private Customer customer(String first, String last, String omang, String address, String phone,
                              String email, String username, Employer employer) {
        String salt = PasswordUtil.generateSalt();
        Customer c = new Customer(null, first, last, omang, address, phone, email, username,
                PasswordUtil.hash(DEMO_PASSWORD, salt), salt);
        c.setEmployer(employer);
        return customerDAO.create(c);
    }

    private void savings(Customer owner, String branch, String deposit) throws BankingException {
        SavingsAccount account = new SavingsAccount(accountDAO.nextAccountNumber(AccountType.SAVINGS), owner, branch);
        account.deposit(new BigDecimal(deposit), "Initial deposit");
        accountDAO.create(account);
    }

    private void investment(Customer owner, String branch, String openingDeposit) throws BankingException {
        InvestmentAccount account = new InvestmentAccount(accountDAO.nextAccountNumber(AccountType.INVESTMENT),
                owner, branch, new BigDecimal(openingDeposit));
        accountDAO.create(account);
    }

    private void cheque(Customer owner, String branch, String salary, String withdrawal) throws BankingException {
        ChequeAccount account = new ChequeAccount(accountDAO.nextAccountNumber(AccountType.CHEQUE), owner, branch);
        account.depositSalary(new BigDecimal(salary));
        accountDAO.create(account);
        Account saved = account;
        Transaction tx = ((Withdrawable) saved).withdraw(new BigDecimal(withdrawal), "ATM withdrawal");
        accountDAO.applyTransaction(saved, tx);
    }
}
