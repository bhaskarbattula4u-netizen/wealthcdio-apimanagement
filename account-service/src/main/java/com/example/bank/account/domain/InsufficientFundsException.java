package com.example.bank.account.domain;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountId, BigDecimal balance, BigDecimal requested) {
        super("Account %s has balance %s, which cannot cover %s".formatted(accountId, balance, requested));
    }
}
