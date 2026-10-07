package com.bank.account.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bank.account.domain.EntryType;
import com.bank.account.domain.LedgerEntryCommand;
import com.bank.account.domain.LedgerEntryView;
import com.bank.account.domain.LedgerUnavailableException;
import com.bank.account.infrastructure.HttpLedgerClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.CREATED;

@DisplayName("HttpLedgerClient")
class HttpLedgerClientTest {

    private MockRestServiceServer server;
    private HttpLedgerClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpLedgerClient(builder, "http://ledger");
    }

    @Test
    @DisplayName("posts the entry as JSON to the ledger")
    void recordsEntry() {
        server.expect(requestTo("http://ledger/ledger/entries"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value("A"))
                .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.counterpartyAccountId").value("B"))
                .andRespond(withStatus(CREATED));

        client.record(new LedgerEntryCommand(
                "A", EntryType.TRANSFER_OUT, new BigDecimal("30.00"), new BigDecimal("70.00"), "B", "corr"));

        server.verify();
    }

    @Test
    @DisplayName("reads an account's history from the ledger")
    void readsHistory() {
        server.expect(requestTo("http://ledger/ledger/accounts/A/entries"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"5d1f5c58-93b6-4a3a-9f0b-0d2f1c1d7a11","accountId":"A","type":"DEPOSIT",
                          "amount":10.00,"balanceAfter":10.00,"counterpartyAccountId":null,
                          "correlationId":"corr","timestamp":"2026-01-15T10:00:00Z"}]
                        """, MediaType.APPLICATION_JSON));

        List<LedgerEntryView> history = client.history("A");

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.type()).isEqualTo(EntryType.DEPOSIT);
            assertThat(entry.amount()).isEqualByComparingTo("10.00");
            assertThat(entry.timestamp()).hasToString("2026-01-15T10:00:00Z");
        });
    }

    @Test
    @DisplayName("reports a ledger error response as LedgerUnavailableException")
    void serverErrorOnRecord() {
        server.expect(requestTo("http://ledger/ledger/entries")).andRespond(withServerError());

        assertThatThrownBy(() -> client.record(new LedgerEntryCommand(
                "A", EntryType.DEPOSIT, BigDecimal.ONE, BigDecimal.ONE, null, "corr")))
                .isInstanceOf(LedgerUnavailableException.class);
    }

    @Test
    @DisplayName("reports a ledger error while reading history as LedgerUnavailableException")
    void serverErrorOnHistory() {
        server.expect(requestTo("http://ledger/ledger/accounts/A/entries")).andRespond(withServerError());

        assertThatThrownBy(() -> client.history("A")).isInstanceOf(LedgerUnavailableException.class);
    }
}
