package com.example.bank.ledger.api;

import com.example.bank.ledger.application.AppendEntryCommand;
import com.example.bank.ledger.domain.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AppendEntryRequest(
        @NotBlank String accountId,
        @NotNull EntryType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull @PositiveOrZero BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId) {

    AppendEntryCommand toCommand() {
        return new AppendEntryCommand(accountId, type, amount, balanceAfter, counterpartyAccountId, correlationId);
    }
}
