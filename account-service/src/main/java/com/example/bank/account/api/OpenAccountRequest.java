package com.example.bank.account.api;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** {@code openingBalance} is optional and defaults to zero. */
public record OpenAccountRequest(@NotBlank String id, BigDecimal openingBalance) {

    BigDecimal openingBalanceOrZero() {
        return openingBalance == null ? BigDecimal.ZERO : openingBalance;
    }
}
