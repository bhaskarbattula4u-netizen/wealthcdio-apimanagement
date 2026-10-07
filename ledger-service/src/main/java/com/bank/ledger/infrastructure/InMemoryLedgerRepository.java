package com.bank.ledger.infrastructure;

import org.springframework.stereotype.Repository;

import com.bank.ledger.domain.LedgerEntry;
import com.bank.ledger.domain.LedgerRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe, append-only, in-memory store. Swap for a database-backed adapter without touching callers. */
@Repository
public class InMemoryLedgerRepository implements LedgerRepository {

    private final Map<String, List<LedgerEntry>> entriesByAccount = new ConcurrentHashMap<>();

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        entriesByAccount
                .computeIfAbsent(entry.accountId(), id -> new CopyOnWriteArrayList<>())
                .add(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findByAccountId(String accountId) {
        return List.copyOf(entriesByAccount.getOrDefault(accountId, List.of()));
    }
}
