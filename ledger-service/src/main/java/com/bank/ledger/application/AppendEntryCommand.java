package com.bank.ledger.application;

import java.math.BigDecimal;

import com.bank.ledger.domain.EntryType;

/** What the account service asks the ledger to record. The ledger assigns the id and timestamp. */
public record AppendEntryCommand(
        String accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId) {
}
