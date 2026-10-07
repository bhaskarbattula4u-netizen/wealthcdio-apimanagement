package com.example.bank.account.domain;

import java.math.BigDecimal;

/** A request to the ledger to record one line of an account's history. */
public record LedgerEntryCommand(
        String accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId) {
}
