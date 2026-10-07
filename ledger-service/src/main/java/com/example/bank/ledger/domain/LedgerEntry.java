package com.example.bank.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One immutable line in an account's ledger.
 *
 * @param amount               always positive; {@link #type} says which direction the money moved
 * @param balanceAfter         the account balance immediately after this entry was applied
 * @param counterpartyAccountId the other side of a transfer, {@code null} for deposits/withdrawals
 * @param correlationId        groups entries that belong to one business operation
 *                             (both legs of a transfer share one)
 */
public record LedgerEntry(
        UUID id,
        String accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountId,
        String correlationId,
        Instant timestamp) {
}
