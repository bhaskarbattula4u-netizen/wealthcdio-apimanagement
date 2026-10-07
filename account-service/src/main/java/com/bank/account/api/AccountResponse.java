package com.bank.account.api;

import java.math.BigDecimal;

public record AccountResponse(String id, BigDecimal balance) {
}
