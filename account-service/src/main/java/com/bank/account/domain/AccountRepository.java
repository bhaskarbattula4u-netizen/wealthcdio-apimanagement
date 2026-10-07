package com.bank.account.domain;

import java.util.Optional;

/** Storage port for accounts. */
public interface AccountRepository {

    Optional<Account> findById(String id);

    /** @return {@code true} if stored, {@code false} if an account with this id already exists (atomic) */
    boolean addIfAbsent(Account account);

    void remove(String id);
}
