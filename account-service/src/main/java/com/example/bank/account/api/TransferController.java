package com.example.bank.account.api;

import com.example.bank.account.application.AccountService;
import com.example.bank.account.application.TransferResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final AccountService service;

    public TransferController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        TransferResult result = service.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return new TransferResponse(
                request.fromAccountId(), result.fromBalance(), request.toAccountId(), result.toBalance());
    }
}
