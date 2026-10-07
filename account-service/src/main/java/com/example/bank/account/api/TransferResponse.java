package com.example.bank.account.api;

import java.math.BigDecimal;

public record TransferResponse(
        String fromAccountId,
        BigDecimal fromBalance,
        String toAccountId,
        BigDecimal toBalance) {
}
