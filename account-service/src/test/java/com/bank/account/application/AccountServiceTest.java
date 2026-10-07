package com.bank.account.application;

import com.bank.account.application.AccountService;
import com.bank.account.application.TransferResult;
import com.bank.account.domain.AccountAlreadyExistsException;
import com.bank.account.domain.AccountNotFoundException;
import com.bank.account.domain.EntryType;
import com.bank.account.domain.InsufficientFundsException;
import com.bank.account.domain.InvalidAmountException;
import com.bank.account.domain.LedgerEntryCommand;
import com.bank.account.domain.LedgerEntryView;
import com.bank.account.domain.LedgerUnavailableException;
import com.bank.account.domain.SameAccountTransferException;
import com.bank.account.infrastructure.InMemoryAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountService")
class AccountServiceTest {

    private FakeLedgerClient ledger;
    private AccountService service;

    @BeforeEach
    void setUp() {
        ledger = new FakeLedgerClient();
        service = new AccountService(new InMemoryAccountRepository(), ledger);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Nested
    @DisplayName("opening an account")
    class Opening {

        @Test
        @DisplayName("creates an account with a unique id and its opening balance")
        void opens() {
            service.open("A", money("100.00"));

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("records a non-zero opening balance as a deposit in the ledger")
        void recordsOpeningDeposit() {
            service.open("A", money("100.00"));

            assertThat(ledger.recorded()).singleElement().satisfies(entry -> {
                assertThat(entry.accountId()).isEqualTo("A");
                assertThat(entry.type()).isEqualTo(EntryType.DEPOSIT);
                assertThat(entry.amount()).isEqualByComparingTo("100.00");
                assertThat(entry.balanceAfter()).isEqualByComparingTo("100.00");
            });
        }

        @Test
        @DisplayName("records nothing for a zero opening balance")
        void zeroOpeningBalanceIsNotLedgered() {
            service.open("A", money("0"));

            assertThat(ledger.recorded()).isEmpty();
        }

        @Test
        @DisplayName("rejects a duplicate id and leaves the original account untouched")
        void rejectsDuplicateId() {
            service.open("A", money("100.00"));

            assertThatThrownBy(() -> service.open("A", money("5.00")))
                    .isInstanceOf(AccountAlreadyExistsException.class);
            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("rejects a negative opening balance")
        void rejectsNegativeOpeningBalance() {
            assertThatThrownBy(() -> service.open("A", money("-1")))
                    .isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> service.balance("A")).isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("does not keep the account if its opening deposit cannot be ledgered")
        void ledgerOutageDuringOpening() {
            ledger.failFromCall(1);

            assertThatThrownBy(() -> service.open("A", money("100.00")))
                    .isInstanceOf(LedgerUnavailableException.class);
            assertThatThrownBy(() -> service.balance("A")).isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deposits")
    class Deposits {

        @BeforeEach
        void openAccount() {
            service.open("A", money("100.00"));
        }

        @Test
        @DisplayName("increase the balance and record a ledger entry carrying the new balance")
        void deposit() {
            BigDecimal after = service.deposit("A", money("25.50"));

            assertThat(after).isEqualByComparingTo("125.50");
            assertThat(service.balance("A")).isEqualByComparingTo("125.50");
            LedgerEntryCommand entry = ledger.recorded().get(1);
            assertThat(entry.type()).isEqualTo(EntryType.DEPOSIT);
            assertThat(entry.amount()).isEqualByComparingTo("25.50");
            assertThat(entry.balanceAfter()).isEqualByComparingTo("125.50");
        }

        @Test
        @DisplayName("reject zero, negative, missing and sub-cent amounts without side effects")
        void rejectInvalidAmounts() {
            for (BigDecimal bad : new BigDecimal[]{money("0"), money("-5"), null, money("1.001")}) {
                assertThatThrownBy(() -> service.deposit("A", bad)).isInstanceOf(InvalidAmountException.class);
            }
            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
            assertThat(ledger.recorded()).hasSize(1);
        }

        @Test
        @DisplayName("to an unknown account fail with not-found")
        void unknownAccount() {
            assertThatThrownBy(() -> service.deposit("nope", money("1.00")))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("are reversed if the ledger cannot record them")
        void ledgerOutageRollsBack() {
            ledger.failFromCall(2);

            assertThatThrownBy(() -> service.deposit("A", money("25.00")))
                    .isInstanceOf(LedgerUnavailableException.class);
            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("withdrawals")
    class Withdrawals {

        @BeforeEach
        void openAccount() {
            service.open("A", money("100.00"));
        }

        @Test
        @DisplayName("decrease the balance and record a ledger entry")
        void withdraw() {
            BigDecimal after = service.withdraw("A", money("40.00"));

            assertThat(after).isEqualByComparingTo("60.00");
            assertThat(ledger.recorded().get(1).type()).isEqualTo(EntryType.WITHDRAWAL);
        }

        @Test
        @DisplayName("may take the whole balance, leaving zero")
        void withdrawEverything() {
            service.withdraw("A", money("100.00"));

            assertThat(service.balance("A")).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("that would overdraw are refused, changing neither balance nor ledger")
        void overdraftRefused() {
            assertThatThrownBy(() -> service.withdraw("A", money("100.01")))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
            assertThat(ledger.recorded()).hasSize(1);
        }

        @Test
        @DisplayName("reject invalid amounts")
        void rejectInvalidAmounts() {
            assertThatThrownBy(() -> service.withdraw("A", money("0"))).isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> service.withdraw("A", money("-1"))).isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("are reversed if the ledger cannot record them")
        void ledgerOutageRollsBack() {
            ledger.failFromCall(2);

            assertThatThrownBy(() -> service.withdraw("A", money("40.00")))
                    .isInstanceOf(LedgerUnavailableException.class);
            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("transfers")
    class Transfers {

        @BeforeEach
        void openAccounts() {
            service.open("A", money("100.00"));
            service.open("B", money("50.00"));
        }

        @Test
        @DisplayName("move money from one account to the other")
        void transfer() {
            TransferResult result = service.transfer("A", "B", money("30.00"));

            assertThat(result.fromBalance()).isEqualByComparingTo("70.00");
            assertThat(result.toBalance()).isEqualByComparingTo("80.00");
            assertThat(service.balance("A")).isEqualByComparingTo("70.00");
            assertThat(service.balance("B")).isEqualByComparingTo("80.00");
        }

        @Test
        @DisplayName("write one ledger entry per account, linked by a shared correlation id")
        void ledgerEntriesForBothSides() {
            service.transfer("A", "B", money("30.00"));

            LedgerEntryCommand out = ledger.recorded().get(2);
            LedgerEntryCommand in = ledger.recorded().get(3);
            assertThat(out.accountId()).isEqualTo("A");
            assertThat(out.type()).isEqualTo(EntryType.TRANSFER_OUT);
            assertThat(out.counterpartyAccountId()).isEqualTo("B");
            assertThat(out.balanceAfter()).isEqualByComparingTo("70.00");
            assertThat(in.accountId()).isEqualTo("B");
            assertThat(in.type()).isEqualTo(EntryType.TRANSFER_IN);
            assertThat(in.counterpartyAccountId()).isEqualTo("A");
            assertThat(in.balanceAfter()).isEqualByComparingTo("80.00");
            assertThat(in.correlationId()).isEqualTo(out.correlationId());
        }

        @Test
        @DisplayName("that the source cannot afford are refused and change nothing")
        void insufficientFunds() {
            assertThatThrownBy(() -> service.transfer("A", "B", money("100.01")))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
            assertThat(service.balance("B")).isEqualByComparingTo("50.00");
            assertThat(ledger.recorded()).hasSize(2);
        }

        @Test
        @DisplayName("to the same account are refused")
        void sameAccount() {
            assertThatThrownBy(() -> service.transfer("A", "A", money("10.00")))
                    .isInstanceOf(SameAccountTransferException.class);
        }

        @Test
        @DisplayName("involving an unknown account are refused and change nothing")
        void unknownAccount() {
            assertThatThrownBy(() -> service.transfer("A", "nope", money("10.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> service.transfer("nope", "A", money("10.00")))
                    .isInstanceOf(AccountNotFoundException.class);

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("reject invalid amounts")
        void invalidAmount() {
            assertThatThrownBy(() -> service.transfer("A", "B", money("0"))).isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> service.transfer("A", "B", null)).isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("are fully reversed if the ledger fails before recording anything")
        void ledgerOutageBeforeAnyEntry() {
            ledger.failFromCall(3);

            assertThatThrownBy(() -> service.transfer("A", "B", money("30.00")))
                    .isInstanceOf(LedgerUnavailableException.class);

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
            assertThat(service.balance("B")).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("are fully reversed if the ledger fails after recording the first leg")
        void ledgerOutageBetweenLegs() {
            ledger.failFromCall(4);

            assertThatThrownBy(() -> service.transfer("A", "B", money("30.00")))
                    .isInstanceOf(LedgerUnavailableException.class);

            assertThat(service.balance("A")).isEqualByComparingTo("100.00");
            assertThat(service.balance("B")).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("history")
    class History {

        @Test
        @DisplayName("lists the account's ledger entries")
        void history() {
            service.open("A", money("100.00"));
            service.open("B", money("0"));
            service.deposit("A", money("10.00"));
            service.transfer("A", "B", money("20.00"));

            List<LedgerEntryView> entries = service.history("A");

            assertThat(entries).extracting(LedgerEntryView::type)
                    .containsExactly(EntryType.DEPOSIT, EntryType.DEPOSIT, EntryType.TRANSFER_OUT);
        }

        @Test
        @DisplayName("fails with not-found for an unknown account")
        void unknownAccount() {
            assertThatThrownBy(() -> service.history("nope")).isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Test
    @DisplayName("balance of an unknown account fails with not-found")
    void balanceOfUnknownAccount() {
        assertThatThrownBy(() -> service.balance("nope")).isInstanceOf(AccountNotFoundException.class);
    }
}
