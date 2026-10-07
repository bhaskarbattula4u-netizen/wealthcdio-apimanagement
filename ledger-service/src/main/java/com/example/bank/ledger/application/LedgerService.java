package com.example.bank.ledger.application;

import com.example.bank.ledger.domain.LedgerEntry;
import com.example.bank.ledger.domain.LedgerRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Owns the ledger's one non-trivial rule: the ledger, not the caller, decides the id and the timestamp,
 * so history cannot be back-dated by a client.
 */
@Service
public class LedgerService {

    private final LedgerRepository repository;
    private final Clock clock;

    public LedgerService(LedgerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LedgerEntry append(AppendEntryCommand command) {
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                command.accountId(),
                command.type(),
                command.amount(),
                command.balanceAfter(),
                command.counterpartyAccountId(),
                command.correlationId(),
                clock.instant());
        return repository.save(entry);
    }

    public List<LedgerEntry> history(String accountId) {
        return repository.findByAccountId(accountId);
    }
}
