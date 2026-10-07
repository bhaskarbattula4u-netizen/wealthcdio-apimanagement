package com.example.bank.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One line of an account's history as the ledger reports it. */
public record LedgerEntryView(
        UUID id,
        String accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId,
        Instant timestamp) {
}
