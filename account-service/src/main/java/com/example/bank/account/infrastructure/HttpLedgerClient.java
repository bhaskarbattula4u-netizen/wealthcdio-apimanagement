package com.example.bank.account.infrastructure;

import com.example.bank.account.domain.LedgerClient;
import com.example.bank.account.domain.LedgerEntryCommand;
import com.example.bank.account.domain.LedgerEntryView;
import com.example.bank.account.domain.LedgerUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Talks to the ledger service over REST. Any transport or HTTP error becomes {@link LedgerUnavailableException}. */
@Component
public class HttpLedgerClient implements LedgerClient {

    private final RestClient client;

    public HttpLedgerClient(RestClient.Builder builder, @Value("${ledger.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void record(LedgerEntryCommand command) {
        try {
            client.post()
                    .uri("/ledger/entries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new LedgerUnavailableException("Could not record ledger entry for account " + command.accountId(), e);
        }
    }

    @Override
    public List<LedgerEntryView> history(String accountId) {
        try {
            List<LedgerEntryView> entries = client.get()
                    .uri("/ledger/accounts/{accountId}/entries", accountId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LedgerEntryView>>() { });
            return entries == null ? List.of() : entries;
        } catch (RestClientException e) {
            throw new LedgerUnavailableException("Could not fetch ledger history for account " + accountId, e);
        }
    }
}
