package com.bank.account.api;

import com.bank.account.application.AccountService;
import com.bank.account.domain.Account;
import com.bank.account.domain.LedgerEntryView;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = service.open(request.id(), request.openingBalanceOrZero());
        return new AccountResponse(account.id(), account.balance());
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable String id) {
        return new AccountResponse(id, service.balance(id));
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable String id) {
        return new BalanceResponse(id, service.balance(id));
    }

    @PostMapping("/{id}/deposits")
    public BalanceResponse deposit(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return new BalanceResponse(id, service.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/withdrawals")
    public BalanceResponse withdraw(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return new BalanceResponse(id, service.withdraw(id, request.amount()));
    }

    @GetMapping("/{id}/transactions")
    public List<LedgerEntryView> transactions(@PathVariable String id) {
        return service.history(id);
    }
}
