package com.example.bank.account.domain;

/** The ledger could not record an operation, so the operation was not applied. */
public class LedgerUnavailableException extends RuntimeException {

    public LedgerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
