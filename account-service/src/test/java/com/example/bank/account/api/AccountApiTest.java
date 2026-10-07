package com.example.bank.account.api;

import com.example.bank.account.application.AccountService;
import com.example.bank.account.application.TransferResult;
import com.example.bank.account.domain.Account;
import com.example.bank.account.domain.AccountAlreadyExistsException;
import com.example.bank.account.domain.AccountNotFoundException;
import com.example.bank.account.domain.EntryType;
import com.example.bank.account.domain.InsufficientFundsException;
import com.example.bank.account.domain.InvalidAmountException;
import com.example.bank.account.domain.LedgerEntryView;
import com.example.bank.account.domain.LedgerUnavailableException;
import com.example.bank.account.domain.SameAccountTransferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The HTTP contract: routes, request validation and the status code each business failure maps to. */
@WebMvcTest({AccountController.class, TransferController.class})
@DisplayName("Account REST API")
class AccountApiTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AccountService service;

    @Test
    @DisplayName("POST /accounts opens an account and returns 201 with its balance")
    void opensAccount() throws Exception {
        given(service.open("A", new BigDecimal("100.00"))).willReturn(new Account("A", new BigDecimal("100.00")));

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"A\",\"openingBalance\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("A"))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    @DisplayName("POST /accounts without an opening balance opens it at zero")
    void openingBalanceDefaultsToZero() throws Exception {
        given(service.open("A", BigDecimal.ZERO)).willReturn(new Account("A", BigDecimal.ZERO));

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    @DisplayName("POST /accounts with a blank id is 400")
    void rejectsBlankId() throws Exception {
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /accounts with an id already in use is 409")
    void duplicateAccountIsConflict() throws Exception {
        given(service.open(eq("A"), any())).willThrow(new AccountAlreadyExistsException("A"));

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"A\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /accounts/{id}/balance returns the balance")
    void queriesBalance() throws Exception {
        given(service.balance("A")).willReturn(new BigDecimal("42.50"));

        mvc.perform(get("/accounts/A/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("A"))
                .andExpect(jsonPath("$.balance").value(42.50));
    }

    @Test
    @DisplayName("GET /accounts/{id}/balance for an unknown account is 404")
    void unknownAccountIsNotFound() throws Exception {
        given(service.balance("nope")).willThrow(new AccountNotFoundException("nope"));

        mvc.perform(get("/accounts/nope/balance")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposits returns the new balance")
    void deposits() throws Exception {
        given(service.deposit("A", new BigDecimal("10.00"))).willReturn(new BigDecimal("110.00"));

        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(110.00));
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdrawals returns the new balance")
    void withdraws() throws Exception {
        given(service.withdraw("A", new BigDecimal("10.00"))).willReturn(new BigDecimal("90.00"));

        mvc.perform(post("/accounts/A/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(90.00));
    }

    @Test
    @DisplayName("an invalid amount is 400")
    void invalidAmountIsBadRequest() throws Exception {
        given(service.deposit(eq("A"), any())).willThrow(new InvalidAmountException("Amount must be greater than zero"));

        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("a missing amount or malformed body is 400")
    void missingAmountIsBadRequest() throws Exception {
        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an overdraft is 422 Unprocessable Entity")
    void overdraftIsUnprocessable() throws Exception {
        given(service.withdraw(eq("A"), any()))
                .willThrow(new InsufficientFundsException("A", new BigDecimal("5.00"), new BigDecimal("10.00")));

        mvc.perform(post("/accounts/A/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /transfers returns both balances")
    void transfers() throws Exception {
        given(service.transfer("A", "B", new BigDecimal("30.00")))
                .willReturn(new TransferResult(new BigDecimal("70.00"), new BigDecimal("80.00")));

        mvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"A\",\"toAccountId\":\"B\",\"amount\":30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromBalance").value(70.00))
                .andExpect(jsonPath("$.toBalance").value(80.00));
    }

    @Test
    @DisplayName("a transfer to the same account is 422")
    void selfTransferIsUnprocessable() throws Exception {
        given(service.transfer(eq("A"), eq("A"), any())).willThrow(new SameAccountTransferException("A"));

        mvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"A\",\"toAccountId\":\"A\",\"amount\":1.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("a transfer missing an account id is 400")
    void transferNeedsBothAccounts() throws Exception {
        mvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"A\",\"amount\":1.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /accounts/{id}/transactions returns the ledger history")
    void transactionHistory() throws Exception {
        given(service.history("A")).willReturn(List.of(new LedgerEntryView(
                UUID.randomUUID(), "A", EntryType.DEPOSIT, new BigDecimal("10.00"), new BigDecimal("10.00"),
                null, "corr", Instant.parse("2026-01-15T10:00:00Z"))));

        mvc.perform(get("/accounts/A/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].timestamp").value("2026-01-15T10:00:00Z"));
    }

    @Test
    @DisplayName("a ledger outage is 503 and the message tells the caller the operation was not applied")
    void ledgerOutageIsServiceUnavailable() throws Exception {
        given(service.deposit(eq("A"), any())).willThrow(new LedgerUnavailableException("down", null));

        mvc.perform(post("/accounts/A/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not applied")));
    }
}
