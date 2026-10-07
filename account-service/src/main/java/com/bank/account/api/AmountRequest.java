package com.bank.account.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Only presence is checked here; what makes an amount valid is decided once, in the domain. */
public record AmountRequest(@NotNull BigDecimal amount) {
}
