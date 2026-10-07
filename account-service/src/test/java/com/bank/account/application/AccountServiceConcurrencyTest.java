package com.bank.account.application;

import com.bank.account.application.AccountService;
import com.bank.account.domain.InsufficientFundsException;
import com.bank.account.infrastructure.InMemoryAccountRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountService under concurrency")
class AccountServiceConcurrencyTest {

    @Test
    @Timeout(30)
    @DisplayName("opposite concurrent transfers neither deadlock nor create or destroy money")
    void oppositeTransfersConserveMoney() throws Exception {
        AccountService service = new AccountService(new InMemoryAccountRepository(), new FakeLedgerClient());
        service.open("A", new BigDecimal("1000.00"));
        service.open("B", new BigDecimal("1000.00"));

        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<Void>> work = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            boolean forward = i % 2 == 0;
            work.add(() -> {
                try {
                    if (forward) {
                        service.transfer("A", "B", new BigDecimal("3.00"));
                    } else {
                        service.transfer("B", "A", new BigDecimal("2.00"));
                    }
                } catch (InsufficientFundsException ignored) {
                    // acceptable: the point is that the total never changes
                }
                return null;
            });
        }
        for (Future<Void> result : pool.invokeAll(work)) {
            result.get();
        }
        pool.shutdown();

        BigDecimal total = service.balance("A").add(service.balance("B"));
        assertThat(total).isEqualByComparingTo("2000.00");
    }

    @Test
    @Timeout(30)
    @DisplayName("concurrent withdrawals can never overdraw an account")
    void concurrentWithdrawalsNeverOverdraw() throws Exception {
        AccountService service = new AccountService(new InMemoryAccountRepository(), new FakeLedgerClient());
        service.open("A", new BigDecimal("100.00"));

        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<Boolean>> work = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            work.add(() -> {
                try {
                    service.withdraw("A", new BigDecimal("10.00"));
                    return true;
                } catch (InsufficientFundsException refused) {
                    return false;
                }
            });
        }
        int succeeded = 0;
        for (Future<Boolean> result : pool.invokeAll(work)) {
            if (result.get()) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(10);
        assertThat(service.balance("A")).isEqualByComparingTo("0.00");
    }
}
