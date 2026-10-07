package com.example.bank.account.domain;

/**
 * Mirrors the ledger service's entry types. It is duplicated rather than shared through a common library
 * on purpose: the two services only agree on the JSON contract, so either can be deployed independently.
 */
public enum EntryType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN
}
