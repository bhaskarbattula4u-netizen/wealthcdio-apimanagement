package com.bank.ledger.domain;

/** The business reason an entry was written to an account's ledger. */
public enum EntryType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN
}
