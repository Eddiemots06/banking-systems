package com.bac.banking.controller.dto;

/**
 * Read-only view of an account handed to the GUI. Views never touch domain objects directly,
 * so they cannot call deposit/withdraw or change balances themselves.
 */
public record AccountSummary(
        String accountNumber,
        String accountType,
        String ownerName,
        String branch,
        String balance,
        String dateOpened,
        boolean canWithdraw,
        boolean earnsInterest,
        String rules) {
}
