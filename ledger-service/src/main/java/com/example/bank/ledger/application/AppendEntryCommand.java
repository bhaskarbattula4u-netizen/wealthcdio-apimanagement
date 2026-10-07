package com.example.bank.ledger.application;

import com.example.bank.ledger.domain.EntryType;

import java.math.BigDecimal;

/** What the account service asks the ledger to record. The ledger assigns the id and timestamp. */
public record AppendEntryCommand(
        String accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId) {
}
