package com.example.bank.ledger.infrastructure;

import com.example.bank.ledger.domain.EntryType;
import com.example.bank.ledger.domain.LedgerEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryLedgerRepository")
class InMemoryLedgerRepositoryTest {

    @Test
    @DisplayName("loses no entries when many threads append to the same account at once")
    void concurrentAppendsAreAllRetained() throws InterruptedException {
        InMemoryLedgerRepository repository = new InMemoryLedgerRepository();
        int appends = 500;
        ExecutorService pool = Executors.newFixedThreadPool(16);

        IntStream.range(0, appends).forEach(i -> pool.submit(() -> repository.save(entry("A"))));
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(repository.findByAccountId("A")).hasSize(appends);
    }

    private static LedgerEntry entry(String accountId) {
        return new LedgerEntry(UUID.randomUUID(), accountId, EntryType.DEPOSIT,
                BigDecimal.ONE, BigDecimal.ONE, null, "corr", Instant.EPOCH);
    }
}
