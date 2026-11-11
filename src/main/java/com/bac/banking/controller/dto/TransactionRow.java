package com.bac.banking.controller.dto;

/** One line of an account statement, already formatted for display. */
public record TransactionRow(String dateTime, String type, String amount, String balanceAfter, String reference) {
}
