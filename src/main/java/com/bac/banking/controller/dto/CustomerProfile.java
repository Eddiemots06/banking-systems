package com.bac.banking.controller.dto;

/** The logged-in customer's details for display (and pre-filling the employer form). */
public record CustomerProfile(
        String fullName,
        String username,
        String nationalId,
        String address,
        String phone,
        String email,
        boolean employed,
        String companyName,
        String companyAddress,
        String jobTitle,
        String totalBalance) {
}
