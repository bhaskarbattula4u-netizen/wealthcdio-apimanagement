package com.example.bank.ledger.application;

import com.example.bank.ledger.domain.EntryType;
import com.example.bank.ledger.domain.LedgerEntry;
import com.example.bank.ledger.infrastructure.InMemoryLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LedgerService")
class LedgerServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private LedgerService ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService(new InMemoryLedgerRepository(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("stamps each appended entry with a fresh id and the ledger's own clock")
    void stampsIdAndTimestamp() {
        LedgerEntry first = ledger.append(deposit("A", "10.00", "10.00"));
        LedgerEntry second = ledger.append(deposit("A", "5.00", "15.00"));

        assertThat(first.timestamp()).isEqualTo(NOW);
        assertThat(first.id()).isNotNull().isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("keeps the details the caller supplied")
    void preservesCommandDetails() {
        LedgerEntry entry = ledger.append(new AppendEntryCommand(
                "A", EntryType.TRANSFER_OUT, new BigDecimal("25.00"), new BigDecimal("75.00"), "B", "corr-1"));

        assertThat(entry.accountId()).isEqualTo("A");
        assertThat(entry.type()).isEqualTo(EntryType.TRANSFER_OUT);
        assertThat(entry.amount()).isEqualByComparingTo("25.00");
        assertThat(entry.balanceAfter()).isEqualByComparingTo("75.00");
        assertThat(entry.counterpartyAccountId()).isEqualTo("B");
        assertThat(entry.correlationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("returns an account's history in the order entries were appended")
    void historyIsInAppendOrder() {
        ledger.append(deposit("A", "10.00", "10.00"));
        ledger.append(deposit("A", "5.00", "15.00"));
        ledger.append(deposit("A", "1.00", "16.00"));

        List<LedgerEntry> history = ledger.history("A");

        assertThat(history).extracting(entry -> entry.balanceAfter().toPlainString())
                .containsExactly("10.00", "15.00", "16.00");
    }

    @Test
    @DisplayName("keeps accounts' histories separate")
    void historiesAreIsolatedPerAccount() {
        ledger.append(deposit("A", "10.00", "10.00"));
        ledger.append(deposit("B", "99.00", "99.00"));

        assertThat(ledger.history("A")).hasSize(1);
        assertThat(ledger.history("B")).hasSize(1);
    }

    @Test
    @DisplayName("returns an empty history for an account with no entries")
    void emptyHistory() {
        assertThat(ledger.history("unknown")).isEmpty();
    }

    @Test
    @DisplayName("hands out a snapshot, so callers cannot alter the stored history")
    void historyIsASnapshot() {
        ledger.append(deposit("A", "10.00", "10.00"));
        List<LedgerEntry> snapshot = ledger.history("A");

        ledger.append(deposit("A", "5.00", "15.00"));

        assertThat(snapshot).hasSize(1);
        assertThat(ledger.history("A")).hasSize(2);
    }

    private static AppendEntryCommand deposit(String accountId, String amount, String balanceAfter) {
        return new AppendEntryCommand(accountId, EntryType.DEPOSIT,
                new BigDecimal(amount), new BigDecimal(balanceAfter), null, "corr");
    }
}
