package com.example.bank.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Amounts")
class AmountsTest {

    @Test
    @DisplayName("accepts a positive amount and normalises it to two decimal places")
    void normalisesToTwoDecimalPlaces() {
        assertThat(Amounts.positive(new BigDecimal("10")).toPlainString()).isEqualTo("10.00");
        assertThat(Amounts.positive(new BigDecimal("0.5")).toPlainString()).isEqualTo("0.50");
    }

    @Test
    @DisplayName("treats trailing zeros beyond two places as harmless (10.500 is 10.50)")
    void trailingZerosAreHarmless() {
        assertThat(Amounts.positive(new BigDecimal("10.500")).toPlainString()).isEqualTo("10.50");
    }

    @Test
    @DisplayName("rejects zero and negative amounts")
    void rejectsZeroAndNegative() {
        assertThatThrownBy(() -> Amounts.positive(BigDecimal.ZERO)).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> Amounts.positive(new BigDecimal("-0.01"))).isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("rejects a missing amount")
    void rejectsNull() {
        assertThatThrownBy(() -> Amounts.positive(null))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("rejects sub-cent precision instead of silently rounding the customer's money")
    void rejectsSubCentPrecision() {
        assertThatThrownBy(() -> Amounts.positive(new BigDecimal("10.005")))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("decimal places");
    }

    @Test
    @DisplayName("allows zero for non-negative amounts but still rejects negatives")
    void nonNegative() {
        assertThat(Amounts.nonNegative(BigDecimal.ZERO).toPlainString()).isEqualTo("0.00");
        assertThatThrownBy(() -> Amounts.nonNegative(new BigDecimal("-1"))).isInstanceOf(InvalidAmountException.class);
    }
}
