package com.bac.banking.controller;

import com.bac.banking.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Shared input checks used by all controllers. Keeps validation out of the views. */
final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{7,12}$");
    private static final Pattern OMANG = Pattern.compile("^\\d{9}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._]{2,29}$");

    private InputValidator() {
    }

    static String required(String value, String field) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static BigDecimal amount(String text) throws ValidationException {
        String cleaned = required(text, "Amount").replace("BWP", "").replace("P", "").replace(",", "").trim();
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() <= 0) {
                throw new ValidationException("Amount must be greater than zero.");
            }
            if (value.stripTrailingZeros().scale() > 2) {
                throw new ValidationException("Amount cannot have more than 2 decimal places.");
            }
            if (value.compareTo(new BigDecimal("10000000")) > 0) {
                throw new ValidationException("Amount is above the BWP 10,000,000 single transaction limit.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException("'" + text.trim() + "' is not a valid amount. Use a number like 250.00");
        }
    }

    /** Opening deposits may be zero (savings/cheque), so only non-negative is required. */
    static BigDecimal openingAmount(String text) throws ValidationException {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        String cleaned = text.replace("BWP", "").replace(",", "").trim();
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0) {
                throw new ValidationException("Opening deposit cannot be negative.");
            }
            if (value.stripTrailingZeros().scale() > 2) {
                throw new ValidationException("Amount cannot have more than 2 decimal places.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException("'" + text.trim() + "' is not a valid amount.");
        }
    }

    static void email(String email) throws ValidationException {
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
    }

    static void phone(String phone) throws ValidationException {
        if (phone != null && !PHONE.matcher(phone.replace(" ", "")).matches()) {
            throw new ValidationException("Phone number must contain 7 to 12 digits.");
        }
    }

    static void omang(String nationalId) throws ValidationException {
        if (!OMANG.matcher(nationalId).matches()) {
            throw new ValidationException("National ID (Omang) must be exactly 9 digits.");
        }
    }

    static void username(String username) throws ValidationException {
        if (!USERNAME.matcher(username).matches()) {
            throw new ValidationException(
                    "Username must start with a letter and be 3-30 letters, digits, dots or underscores.");
        }
    }

    static void password(String password, String confirm) throws ValidationException {
        required(password, "Password");
        if (password.length() < 8 || !password.matches(".*\\d.*") || !password.matches(".*[A-Za-z].*")) {
            throw new ValidationException("Password must be at least 8 characters and contain letters and numbers.");
        }
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
    }
}
