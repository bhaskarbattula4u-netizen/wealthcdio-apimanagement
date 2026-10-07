package com.bank.account.domain;

import java.util.List;

/** Port to the ledger service. */
public interface LedgerClient {

    /** @throws LedgerUnavailableException if the entry could not be recorded */
    void record(LedgerEntryCommand command);

    /** @throws LedgerUnavailableException if the history could not be fetched */
    List<LedgerEntryView> history(String accountId);
}
