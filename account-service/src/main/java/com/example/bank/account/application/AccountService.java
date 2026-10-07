package com.example.bank.account.application;

import com.example.bank.account.domain.Account;
import com.example.bank.account.domain.AccountAlreadyExistsException;
import com.example.bank.account.domain.AccountNotFoundException;
import com.example.bank.account.domain.AccountRepository;
import com.example.bank.account.domain.Amounts;
import com.example.bank.account.domain.EntryType;
import com.example.bank.account.domain.LedgerClient;
import com.example.bank.account.domain.LedgerEntryCommand;
import com.example.bank.account.domain.LedgerEntryView;
import com.example.bank.account.domain.LedgerUnavailableException;
import com.example.bank.account.domain.SameAccountTransferException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates account operations and keeps the ledger in step with balances.
 *
 * <p><b>Consistency rule:</b> an operation either changes the balance <em>and</em> is recorded in the
 * ledger, or does neither. The ledger write happens after the balance change; if the ledger is
 * unavailable the balance change is reversed and {@link LedgerUnavailableException} is thrown.
 *
 * <p>Each operation holds the monitor of the account(s) it touches for its whole duration, including the
 * ledger call, so ledger entries for an account are written in the same order as its balance changes.
 * For transfers the two monitors are always taken in id order so two opposite transfers cannot deadlock.
 */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final LedgerClient ledger;

    public AccountService(AccountRepository accounts, LedgerClient ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public Account open(String id, BigDecimal openingBalance) {
        Account account = new Account(id, openingBalance);
        if (!accounts.addIfAbsent(account)) {
            throw new AccountAlreadyExistsException(id);
        }
        if (account.balance().signum() > 0) {
            try {
                ledger.record(new LedgerEntryCommand(
                        id, EntryType.DEPOSIT, account.balance(), account.balance(), null, newCorrelationId()));
            } catch (LedgerUnavailableException e) {
                accounts.remove(id);
                throw e;
            }
        }
        return account;
    }

    public BigDecimal balance(String id) {
        return find(id).balance();
    }

    public BigDecimal deposit(String id, BigDecimal amount) {
        BigDecimal valid = Amounts.positive(amount);
        Account account = find(id);
        synchronized (account) {
            BigDecimal balanceAfter = account.deposit(valid);
            try {
                ledger.record(new LedgerEntryCommand(
                        id, EntryType.DEPOSIT, valid, balanceAfter, null, newCorrelationId()));
            } catch (LedgerUnavailableException e) {
                account.withdraw(valid);
                throw e;
            }
            return balanceAfter;
        }
    }

    public BigDecimal withdraw(String id, BigDecimal amount) {
        BigDecimal valid = Amounts.positive(amount);
        Account account = find(id);
        synchronized (account) {
            BigDecimal balanceAfter = account.withdraw(valid);
            try {
                ledger.record(new LedgerEntryCommand(
                        id, EntryType.WITHDRAWAL, valid, balanceAfter, null, newCorrelationId()));
            } catch (LedgerUnavailableException e) {
                account.deposit(valid);
                throw e;
            }
            return balanceAfter;
        }
    }

    public TransferResult transfer(String fromId, String toId, BigDecimal amount) {
        BigDecimal valid = Amounts.positive(amount);
        if (fromId.equals(toId)) {
            throw new SameAccountTransferException(fromId);
        }
        Account from = find(fromId);
        Account to = find(toId);
        Account first = fromId.compareTo(toId) < 0 ? from : to;
        Account second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                BigDecimal fromBalance = from.withdraw(valid);
                BigDecimal toBalance = to.deposit(valid);
                String correlationId = newCorrelationId();
                try {
                    ledger.record(new LedgerEntryCommand(
                            fromId, EntryType.TRANSFER_OUT, valid, fromBalance, toId, correlationId));
                    ledger.record(new LedgerEntryCommand(
                            toId, EntryType.TRANSFER_IN, valid, toBalance, fromId, correlationId));
                } catch (LedgerUnavailableException e) {
                    to.withdraw(valid);
                    from.deposit(valid);
                    throw e;
                }
                return new TransferResult(fromBalance, toBalance);
            }
        }
    }

    public List<LedgerEntryView> history(String id) {
        find(id);
        return ledger.history(id);
    }

    private Account find(String id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
