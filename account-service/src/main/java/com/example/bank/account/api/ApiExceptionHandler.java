package com.example.bank.account.api;

import com.example.bank.account.domain.AccountAlreadyExistsException;
import com.example.bank.account.domain.AccountNotFoundException;
import com.example.bank.account.domain.InsufficientFundsException;
import com.example.bank.account.domain.InvalidAmountException;
import com.example.bank.account.domain.LedgerUnavailableException;
import com.example.bank.account.domain.SameAccountTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * The one place that maps business failures to HTTP, as RFC 7807 problem documents.
 *
 * <ul>
 *   <li>400 - the request itself is malformed or the amount is invalid</li>
 *   <li>404 - an account does not exist</li>
 *   <li>409 - the account id is already taken</li>
 *   <li>422 - the request is well-formed but the business rules refuse it (overdraft, self-transfer)</li>
 *   <li>503 - the ledger is down, so the operation was rolled back and may be retried</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({InvalidAmountException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    ProblemDetail conflict(AccountAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InsufficientFundsException.class, SameAccountTransferException.class})
    ProblemDetail unprocessable(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(LedgerUnavailableException.class)
    ProblemDetail ledgerUnavailable(LedgerUnavailableException e) {
        log.warn("Ledger unavailable, operation rolled back: {}", e.getMessage(), e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "The transaction could not be recorded and was not applied. Please retry.");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
