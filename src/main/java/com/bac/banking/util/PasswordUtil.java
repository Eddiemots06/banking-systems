package com.bac.banking.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted SHA-256 password hashing so that plain-text passwords are never stored.
 */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10_000;

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String plainPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = (salt + plainPassword).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < ITERATIONS; i++) {
                digest.reset();
                result = digest.digest(result);
            }
            return Base64.getEncoder().encodeToString(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String plainPassword, String salt, String expectedHash) {
        byte[] actual = hash(plainPassword, salt).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }
}
