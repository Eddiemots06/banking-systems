package com.bac.banking.controller;

import com.bac.banking.controller.dto.CustomerProfile;
import com.bac.banking.dao.AccountDAO;
import com.bac.banking.dao.CustomerDAO;
import com.bac.banking.exception.ValidationException;
import com.bac.banking.model.Customer;
import com.bac.banking.model.Employer;
import com.bac.banking.util.MoneyFormat;
import com.bac.banking.util.PasswordUtil;

/**
 * Handles the Register Customer and Update Employment use cases.
 */
public class CustomerController {
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final Session session;

    public CustomerController(CustomerDAO customerDAO, AccountDAO accountDAO, Session session) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
        this.session = session;
    }

    /**
     * Validates the registration form and saves the new customer.
     *
     * @return the username the customer will log in with
     */
    public String register(String firstName, String surname, String nationalId, String address,
                           String phone, String email, String username, String password, String confirmPassword,
                           boolean employed, String companyName, String companyAddress, String jobTitle)
            throws ValidationException {
        String first = InputValidator.required(firstName, "First name");
        String last = InputValidator.required(surname, "Surname");
        String omang = InputValidator.required(nationalId, "National ID (Omang)");
        String addr = InputValidator.required(address, "Address");
        String user = InputValidator.required(username, "Username").toLowerCase();
        String tel = InputValidator.optional(phone);
        String mail = InputValidator.optional(email);

        InputValidator.omang(omang);
        InputValidator.username(user);
        InputValidator.phone(tel);
        InputValidator.email(mail);
        InputValidator.password(password, confirmPassword);

        if (customerDAO.usernameExists(user)) {
            throw new ValidationException("The username '" + user + "' is already taken.");
        }
        if (customerDAO.nationalIdExists(omang)) {
            throw new ValidationException("A customer with this National ID is already registered.");
        }

        String salt = PasswordUtil.generateSalt();
        Customer customer = new Customer(null, first, last, omang, addr, tel, mail, user,
                PasswordUtil.hash(password, salt), salt);
        if (employed) {
            customer.setEmployer(buildEmployer(companyName, companyAddress, jobTitle));
        }
        customerDAO.create(customer);
        return customer.getUsername();
    }

    /** Adds or changes employer details for the logged-in customer (needed for a Cheque Account). */
    public void updateEmployment(String companyName, String companyAddress, String jobTitle) throws ValidationException {
        Customer customer = currentCustomer();
        customer.setEmployer(buildEmployer(companyName, companyAddress, jobTitle));
        customerDAO.update(customer);
    }

    /** Removes employer details (e.g. the customer has stopped working). */
    public void clearEmployment() throws ValidationException {
        Customer customer = currentCustomer();
        customer.setEmployer(null);
        customerDAO.update(customer);
    }

    public CustomerProfile getProfile() throws ValidationException {
        Customer c = currentCustomer();
        accountDAO.findByCustomer(c);
        Employer e = c.getEmployer();
        return new CustomerProfile(c.getDisplayName(), c.getUsername(), c.getNationalId(), c.getAddress(),
                c.getPhone(), c.getEmail(), c.isEmployed(),
                e == null ? "" : e.getCompanyName(),
                e == null ? "" : e.getCompanyAddress(),
                e == null || e.getJobTitle() == null ? "" : e.getJobTitle(),
                MoneyFormat.bwp(c.getTotalBalance()));
    }

    private Employer buildEmployer(String companyName, String companyAddress, String jobTitle)
            throws ValidationException {
        return new Employer(
                InputValidator.required(companyName, "Company name"),
                InputValidator.required(companyAddress, "Company address"),
                InputValidator.optional(jobTitle));
    }

    private Customer currentCustomer() throws ValidationException {
        if (!session.isCustomer()) {
            throw new ValidationException("Please log in as a customer first.");
        }
        return customerDAO.findById(session.getUserId())
                .orElseThrow(() -> new ValidationException("Your customer record could not be found."));
    }
}
