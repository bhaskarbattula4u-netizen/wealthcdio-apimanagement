package com.bank.account.domain;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException(String accountId) {
        super("Cannot transfer from account %s to itself".formatted(accountId));
    }
}
