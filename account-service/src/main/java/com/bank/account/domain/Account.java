package com.bank.account.domain;

import java.math.BigDecimal;

/**
 * A bank account: an id and a balance that can never go below zero.
 *
 * <p>The invariant (no overdraft, no invalid amount) lives here, so no caller can bypass it. Methods are
 * {@code synchronized} so a single account is safe under concurrent use; operations spanning two accounts
 * (transfers) take both monitors in a fixed order in {@code AccountService}.
 */
public final class Account {

    private final String id;
    private BigDecimal balance;

    public Account(String id, BigDecimal openingBalance) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Account id is required");
        }
        this.id = id;
        this.balance = Amounts.nonNegative(openingBalance);
    }

    public String id() {
        return id;
    }

    public synchronized BigDecimal balance() {
        return balance;
    }

    /** @return the balance after the deposit */
    public synchronized BigDecimal deposit(BigDecimal amount) {
        balance = balance.add(Amounts.positive(amount));
        return balance;
    }

    /**
     * @return the balance after the withdrawal
     * @throws InsufficientFundsException if the balance cannot cover the amount (balance is left untouched)
     */
    public synchronized BigDecimal withdraw(BigDecimal amount) {
        BigDecimal valid = Amounts.positive(amount);
        if (balance.compareTo(valid) < 0) {
            throw new InsufficientFundsException(id, balance, valid);
        }
        balance = balance.subtract(valid);
        return balance;
    }
}
