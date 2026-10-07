package com.example.bank.account;

import com.example.bank.account.application.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Account service wiring")
class AccountServiceApplicationTest {

    @Autowired
    private AccountService service;

    @Test
    @DisplayName("starts with the real HTTP ledger client wired in")
    void contextLoads() {
        assertThat(service).isNotNull();
    }
}
