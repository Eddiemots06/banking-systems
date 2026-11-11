package com.bac.banking.controller;

import com.bac.banking.dao.AdministratorDAO;
import com.bac.banking.dao.CustomerDAO;
import com.bac.banking.exception.ValidationException;
import com.bac.banking.model.Administrator;
import com.bac.banking.model.Customer;
import com.bac.banking.model.User;

import java.util.Optional;

/**
 * Handles the Login use case: validates input, authenticates against the stored
 * salted hash and starts the session. Customers and administrators share one login screen.
 */
public class LoginController {
    static final int MAX_ATTEMPTS = 3;

    private final CustomerDAO customerDAO;
    private final AdministratorDAO administratorDAO;
    private final Session session;
    private int failedAttempts;

    public LoginController(CustomerDAO customerDAO, AdministratorDAO administratorDAO, Session session) {
        this.customerDAO = customerDAO;
        this.administratorDAO = administratorDAO;
        this.session = session;
    }

    /**
     * @return the role of the user who logged in, so the view knows which screen to show
     * @throws ValidationException with a user-friendly message if login fails
     */
    public Session.Role login(String username, String password) throws ValidationException {
        if (failedAttempts >= MAX_ATTEMPTS) {
            throw new ValidationException("Too many failed attempts. Please restart the application or contact the bank.");
        }
        String user = InputValidator.required(username, "Username");
        InputValidator.required(password, "Password");

        Optional<Administrator> admin = administratorDAO.findByUsername(user);
        if (admin.isPresent() && admin.get().authenticate(password)) {
            return succeed(admin.get(), admin.get().getAdminId(), Session.Role.ADMINISTRATOR);
        }
        Optional<Customer> customer = customerDAO.findByUsername(user);
        if (customer.isPresent() && customer.get().authenticate(password)) {
            return succeed(customer.get(), customer.get().getCustomerId(), Session.Role.CUSTOMER);
        }
        failedAttempts++;
        int left = MAX_ATTEMPTS - failedAttempts;
        // Same message whether the username or the password is wrong, so usernames cannot be guessed.
        throw new ValidationException("Invalid username or password." + (left > 0 ? " Attempts left: " + left : ""));
    }

    private Session.Role succeed(User user, Long id, Session.Role role) {
        failedAttempts = 0;
        session.start(id, user.getUsername(), user.getDisplayName(), role);
        return role;
    }

    public void logout() {
        session.clear();
    }

    public String getLoggedInName() {
        return session.getDisplayName();
    }
}
