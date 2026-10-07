package com.bank.account.domain;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Account %s does not exist".formatted(accountId));
    }
}
