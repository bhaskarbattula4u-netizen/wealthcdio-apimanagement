package com.example.bank.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account")
class AccountTest {

    @Test
    @DisplayName("opens with the given id and balance")
    void opens() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        assertThat(account.id()).isEqualTo("ACC-1");
        assertThat(account.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("cannot be opened without an id or with a negative balance")
    void rejectsInvalidOpening() {
        assertThatThrownBy(() -> new Account(" ", BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Account(null, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Account("ACC-1", new BigDecimal("-1"))).isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("deposit increases the balance and returns the new balance")
    void deposit() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        BigDecimal after = account.deposit(new BigDecimal("25.50"));

        assertThat(after).isEqualByComparingTo("125.50");
        assertThat(account.balance()).isEqualByComparingTo("125.50");
    }

    @Test
    @DisplayName("withdraw decreases the balance and returns the new balance")
    void withdraw() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        BigDecimal after = account.withdraw(new BigDecimal("40.25"));

        assertThat(after).isEqualByComparingTo("59.75");
    }

    @Test
    @DisplayName("withdrawing exactly the balance is allowed and leaves zero")
    void withdrawEntireBalance() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("100.00"));

        assertThat(account.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("refuses to overdraw and leaves the balance untouched")
    void refusesOverdraft() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("rejects invalid amounts on deposit and withdraw without changing the balance")
    void rejectsInvalidAmounts() {
        Account account = new Account("ACC-1", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO)).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-5"))).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> account.withdraw(null)).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("1.001"))).isInstanceOf(InvalidAmountException.class);
        assertThat(account.balance()).isEqualByComparingTo("100.00");
    }
}
