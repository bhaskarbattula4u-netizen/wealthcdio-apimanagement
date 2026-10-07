# Banking Transaction Processor

A small banking backend built as two Spring Boot microservices (Java 21, Spring Boot 3.3):

| Service | Port | Responsibility |
|---|---|---|
| `account-service` | 8081 | Accounts, balances, deposit / withdraw / transfer, validation. Calls the ledger for every operation. |
| `ledger-service` | 8082 | Append-only, timestamped history of entries per account. |

## IMPORTANT: verification status (please read first)

The environment this was written in could not reach Maven Central, so **`mvn` was never run**: the Spring layers
(controllers, `@WebMvcTest` / `@SpringBootTest` tests, `HttpLedgerClient`, Spring config) and **all JUnit tests have not been compiled or executed**.

What *was* verified: the framework-free core (`domain`, `application` and `InMemory*Repository` classes of both services) compiles with plain `javac`
(Spring's `@Service`/`@Repository` stubbed), and a scratch scenario check of `AccountService` passed: normalisation, duplicate id, overdraft,
sub-cent rejection, transfer, self-transfer, unknown account, ledger-outage rollback, and 2000 concurrent opposite transfers conserving money
with no deadlock. That scratch check is not part of this repo.

Please run `mvn verify` first. If a test fails to compile it is most likely a typo in test code rather than a design issue, and I would rather you
know than assume it is green.

Also be aware that the git history is organised by vertical slice (domain, then service, then API), with implementation and its tests committed together.
I did not do a red/green TDD loop, because I could not run the tests, and I do not want the history to claim otherwise.

## Run it

```bash
mvn verify                                   # build and test both services
mvn -pl ledger-service  spring-boot:run      # terminal 1, port 8082
mvn -pl account-service spring-boot:run      # terminal 2, port 8081 (ledger URL: ledger.base-url)
```

```bash
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' -d '{"id":"A","openingBalance":100.00}'
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' -d '{"id":"B"}'
curl -X POST localhost:8081/accounts/A/deposits    -H 'Content-Type: application/json' -d '{"amount":25.50}'
curl -X POST localhost:8081/accounts/A/withdrawals -H 'Content-Type: application/json' -d '{"amount":10.00}'
curl -X POST localhost:8081/transfers -H 'Content-Type: application/json' -d '{"fromAccountId":"A","toAccountId":"B","amount":30.00}'
curl localhost:8081/accounts/A/balance
curl localhost:8081/accounts/A/transactions
```

## API

`account-service`

| Method & path | Purpose | Success |
|---|---|---|
| `POST /accounts` `{id, openingBalance?}` | Open an account | 201 |
| `GET /accounts/{id}` , `GET /accounts/{id}/balance` | Query balance | 200 |
| `POST /accounts/{id}/deposits` `{amount}` | Deposit | 200 + new balance |
| `POST /accounts/{id}/withdrawals` `{amount}` | Withdraw | 200 + new balance |
| `POST /transfers` `{fromAccountId, toAccountId, amount}` | Transfer | 200 + both balances |
| `GET /accounts/{id}/transactions` | Transaction history (from ledger) | 200 |

`ledger-service` (called by account-service, not by end users)

| Method & path | Purpose |
|---|---|
| `POST /ledger/entries` | Append an entry (server assigns id and timestamp) |
| `GET /ledger/accounts/{id}/entries` | Entries for an account, oldest first |

Errors are RFC 7807 problem documents: **400** invalid amount or malformed request, **404** unknown account, **409** duplicate account id,
**422** overdraft or transfer to the same account, **503** ledger unavailable (the operation was *not* applied; safe to retry).

## Understanding the problem: decisions and edge cases

- **Money is `BigDecimal`, exactly two decimal places.** `Amounts` is the single definition of a valid amount. Zero, negative and missing amounts are
  rejected. Sub-cent precision (e.g. `10.005`) is **rejected, not rounded**: silently rounding a customer's money is worse than refusing. `10.500` is accepted as `10.50`.
- **The no-overdraft rule lives in `Account`**, not the controller, so no caller can bypass it. Withdrawing exactly the balance is allowed.
- **Transfers:** same-account transfers are refused (422); both accounts must exist; money is conserved; both ledger entries share a `correlationId`.
- **Opening balance** may be zero but not negative; a non-zero opening balance is recorded in the ledger as a deposit, so the ledger always sums to the balance.
- **Ledger timestamps are assigned by the ledger** (injected `Clock`), so a client cannot back-date history.

## Why two services, and the trade-offs

I split the **ledger** from **accounts** because they have different shapes: accounts are mutable state with strong invariants; the ledger is append-only and
read-heavy. They only share a JSON contract (`EntryType` is deliberately duplicated rather than put in a shared library, so they can be deployed independently).

Alternatives I considered:
- *One service with a ledger package.* Simpler, and honestly the better default for this scope. I took the microservice route because you asked for it, and the cost is the consistency problem below.
- *Event-driven (account service publishes, ledger consumes).* Better availability, but you need a broker and eventual consistency for balance-vs-history. Too heavy for a 3-4 hour exercise.

**Consistency approach: synchronous call with compensation.** After changing a balance, the account service writes the ledger entry; if that fails, the
balance change is reversed and the caller gets 503. Balances and ledger therefore never disagree about a *completed* operation.

## Known limitations (what I would do with more time)

1. **Partial transfer ledger write.** If the ledger accepts the `TRANSFER_OUT` entry but fails on `TRANSFER_IN`, balances are rolled back but an orphan
   `TRANSFER_OUT` entry remains in the ledger. Fix: a transactional outbox, or a ledger endpoint that accepts both legs atomically.
2. **State is in memory.** Restarting either service loses data. The repositories are ports (`AccountRepository`, `LedgerRepository`), so a JPA/Postgres adapter is a drop-in.
3. **Remote call under a lock.** Each operation holds the account monitor while calling the ledger, which keeps ledger order identical to balance order but means a slow ledger
   slows that account. A 2 s timeout (`ledger.timeout`) bounds it. This also relies on a single account-service instance; multiple instances would need DB row locks or optimistic locking.
4. **No idempotency keys.** A client retrying after a timeout could apply an operation twice. A production API needs an `Idempotency-Key` header.
5. **Account opening has a small window** between registering the account and recording its opening deposit during which it is visible; if the ledger then fails, the account is removed.
6. **No authentication, pagination of history, currencies, or service discovery / resilience library** (retries, circuit breaker).
7. **Broad `IllegalArgumentException` -> 400 mapping** is convenient (blank account id) but could mask a programming error as a client error.
8. **No cross-service end-to-end test** (starting both apps together); the ledger client is covered against a mock server instead.

## Layout

```
account-service/   domain (Account, Amounts, ports) / application (AccountService) / infrastructure / api
ledger-service/    domain (LedgerEntry, port) / application (LedgerService) / infrastructure / api
```
