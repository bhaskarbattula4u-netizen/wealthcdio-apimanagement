package com.bank.account.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.bank.account.domain.LedgerClient;
import com.bank.account.domain.LedgerEntryCommand;
import com.bank.account.domain.LedgerEntryView;
import com.bank.account.domain.LedgerUnavailableException;

/** In-memory stand-in for the ledger service that can be told to start failing. */
class FakeLedgerClient implements LedgerClient {

    private final List<LedgerEntryCommand> recorded = new CopyOnWriteArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int failFromCall = Integer.MAX_VALUE;

    /** The Nth call to {@link #record} (1-based) and every call after it will fail. */
    void failFromCall(int n) {
        failFromCall = n;
    }

    List<LedgerEntryCommand> recorded() {
        return recorded;
    }

    @Override
    public void record(LedgerEntryCommand command) {
        if (calls.incrementAndGet() >= failFromCall) {
            throw new LedgerUnavailableException("ledger is down", null);
        }
        recorded.add(command);
    }

    @Override
    public List<LedgerEntryView> history(String accountId) {
        return recorded.stream()
                .filter(c -> c.accountId().equals(accountId))
                .map(c -> new LedgerEntryView(UUID.randomUUID(), c.accountId(), c.type(), c.amount(),
                        c.balanceAfter(), c.counterpartyAccountId(), c.correlationId(), Instant.EPOCH))
                .toList();
    }
}
