package com.bank.account.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single place that decides what a valid money amount is.
 *
 * <p>Amounts are normalised to exactly two decimal places. An amount with more precision than that
 * (e.g. 10.005) is rejected rather than silently rounded, because rounding someone's money without
 * telling them is worse than refusing the request.
 */
public final class Amounts {

    private static final int SCALE = 2;

    private Amounts() {
    }

    /** A strictly positive amount, as required for every deposit, withdrawal and transfer. */
    public static BigDecimal positive(BigDecimal amount) {
        BigDecimal normalised = normalise(amount);
        if (normalised.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero but was " + amount);
        }
        return normalised;
    }

    /** Zero or more, as allowed for an opening balance. */
    public static BigDecimal nonNegative(BigDecimal amount) {
        BigDecimal normalised = normalise(amount);
        if (normalised.signum() < 0) {
            throw new InvalidAmountException("Amount must not be negative but was " + amount);
        }
        return normalised;
    }

    private static BigDecimal normalise(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        try {
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException tooPrecise) {
            throw new InvalidAmountException("Amount must have at most %d decimal places but was %s"
                    .formatted(SCALE, amount.toPlainString()));
        }
    }
}
