package com.example.bank.ledger.domain;

import java.util.List;

/** Storage port for ledger entries. Entries are append-only: there is deliberately no update or delete. */
public interface LedgerRepository {

    LedgerEntry save(LedgerEntry entry);

    /** Entries for the account in the order they were appended; empty if the account has none. */
    List<LedgerEntry> findByAccountId(String accountId);
}
