package com.example.bank.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }

    /** Injected rather than calling Instant.now() so tests can pin time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
