package com.bac.banking.model;

import com.bac.banking.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for Customer, the abstract User and password hashing. */
class CustomerTest {

    private static Customer customer(String password) {
        String salt = PasswordUtil.generateSalt();
        return new Customer(1L, "Kagiso", "Molefe", "123456781", "Gaborone", "71234561", "k@mail.bw",
                "KMolefe", PasswordUtil.hash(password, salt), salt);
    }

    @Test
    @DisplayName("Authenticate accepts the right password only")
    void authenticate() {
        Customer c = customer("Password@123");
        assertTrue(c.authenticate("Password@123"));
        assertFalse(c.authenticate("password@123"));
        assertFalse(c.authenticate(null));
    }

    @Test
    @DisplayName("Password is never stored in plain text")
    void passwordHashed() {
        Customer c = customer("Password@123");
        assertFalse(c.getPasswordHash().contains("Password@123"));
    }

    @Test
    @DisplayName("Username is normalised to lower case")
    void usernameLowerCase() {
        assertEquals("kmolefe", customer("Password@123").getUsername());
    }

    @Test
    @DisplayName("Required fields are enforced (encapsulation)")
    void requiredFields() {
        Customer c = customer("Password@123");
        assertThrows(IllegalArgumentException.class, () -> c.setFirstName(" "));
        assertThrows(IllegalArgumentException.class, () -> c.setAddress(null));
    }

    @Test
    @DisplayName("Overridden getRole/getDisplayName differ for Customer and Administrator")
    void polymorphicUser() {
        User customer = customer("Password@123");
        User admin = new Administrator(1L, "admin", "Bank Administrator", "h", "s");
        assertEquals("Customer", customer.getRole());
        assertEquals("Administrator", admin.getRole());
        assertEquals("Kagiso Molefe", customer.getDisplayName());
    }

    @Test
    @DisplayName("Employment status follows employer details")
    void employment() {
        Customer c = customer("Password@123");
        assertFalse(c.isEmployed());
        c.setEmployer(new Employer("Kalahari Mining Ltd", "Main Mall", null));
        assertTrue(c.isEmployed());
    }
}
