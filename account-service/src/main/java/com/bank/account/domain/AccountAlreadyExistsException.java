package com.bank.account.domain;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(String accountId) {
        super("Account %s already exists".formatted(accountId));
    }
}
