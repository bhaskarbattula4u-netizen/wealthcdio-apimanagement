package com.bank.account.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
class LedgerClientConfig {

    /**
     * A bounded wait on the ledger: a hung ledger must fail the request (and roll it back) rather than
     * hold an account's lock indefinitely.
     */
    @Bean
    RestClientCustomizer ledgerTimeouts(@Value("${ledger.timeout:2s}") Duration timeout) {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeout);
            factory.setReadTimeout(timeout);
            builder.requestFactory(factory);
        };
    }
}
