package com.bac.banking.model;

/** Kinds of movement that can be recorded against an account. */
public enum TransactionType {
    OPENING_DEPOSIT,
    DEPOSIT,
    WITHDRAWAL,
    INTEREST
}
