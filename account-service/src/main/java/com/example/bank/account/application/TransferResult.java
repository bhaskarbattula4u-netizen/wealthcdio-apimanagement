package com.example.bank.account.application;

import java.math.BigDecimal;

/** The balances of both accounts after a successful transfer. */
public record TransferResult(BigDecimal fromBalance, BigDecimal toBalance) {
}
