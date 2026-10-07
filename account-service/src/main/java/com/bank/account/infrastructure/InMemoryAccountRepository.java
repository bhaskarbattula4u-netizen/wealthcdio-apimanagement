package com.bank.account.infrastructure;

import org.springframework.stereotype.Repository;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public boolean addIfAbsent(Account account) {
        return accounts.putIfAbsent(account.id(), account) == null;
    }

    @Override
    public void remove(String id) {
        accounts.remove(id);
    }
}
