package com.shan.cyber.tech.financetracker.shared.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.exception.CurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void of_withStringAmount_parsesCorrectly() {
        Money money = Money.of("100.50", "USD");
        assertEquals(new BigDecimal("100.5000"), money.amount());
        assertEquals("USD", money.currency());
    }

    @Test
    void of_withBigDecimalAmount_setsScaleTo4() {
        Money money = Money.of(new BigDecimal("50.1"), "EUR");
        assertEquals(new BigDecimal("50.1000"), money.amount());
    }

    @Test
    void zero_createsZeroAmount() {
        Money money = Money.zero("GBP");
        assertTrue(money.isZero());
        assertEquals("GBP", money.currency());
    }

    @Test
    void add_sameCurrency_returnsCorrectSum() {
        Money a = Money.of("100.00", "USD");
        Money b = Money.of("50.25", "USD");
        Money result = a.add(b);
        assertEquals(Money.of("150.25", "USD"), result);
    }

    @Test
    void add_differentCurrency_throwsCurrencyMismatch() {
        Money usd = Money.of("100", "USD");
        Money eur = Money.of("100", "EUR");
        assertThrows(CurrencyMismatchException.class, () -> usd.add(eur));
    }

    @Test
    void subtract_sameCurrency_returnsDifference() {
        Money a = Money.of("200.00", "USD");
        Money b = Money.of("75.50", "USD");
        Money result = a.subtract(b);
        assertEquals(Money.of("124.50", "USD"), result);
    }

    @Test
    void subtract_differentCurrency_throwsCurrencyMismatch() {
        Money usd = Money.of("100", "USD");
        Money eur = Money.of("50", "EUR");
        assertThrows(CurrencyMismatchException.class, () -> usd.subtract(eur));
    }

    @Test
    void negate_returnsNegativeOfPositive() {
        Money money = Money.of("100.00", "USD");
        Money negated = money.negate();
        assertTrue(negated.isNegative());
        assertEquals(Money.of("-100.00", "USD"), negated);
    }

    @Test
    void negate_negativeBecomesPositive() {
        Money money = Money.of("-50.00", "USD");
        Money negated = money.negate();
        assertTrue(negated.isPositive());
    }

    @Test
    void isNegative_whenNegative_returnsTrue() {
        assertTrue(Money.of("-0.01", "USD").isNegative());
    }

    @Test
    void isNegative_whenZero_returnsFalse() {
        assertFalse(Money.zero("USD").isNegative());
    }

    @Test
    void isPositive_whenPositive_returnsTrue() {
        assertTrue(Money.of("0.01", "USD").isPositive());
    }

    @Test
    void isPositive_whenZero_returnsFalse() {
        assertFalse(Money.zero("USD").isPositive());
    }

    @Test
    void isZero_whenZero_returnsTrue() {
        assertTrue(Money.zero("USD").isZero());
    }

    @Test
    void isZero_whenNonZero_returnsFalse() {
        assertFalse(Money.of("0.0001", "USD").isZero());
    }

    @Test
    void equals_sameAmountAndCurrency_returnsTrue() {
        Money a = Money.of("100.00", "USD");
        Money b = Money.of("100.0000", "USD");
        assertEquals(a, b);
    }

    @Test
    void equals_differentCurrency_returnsFalse() {
        Money usd = Money.of("100", "USD");
        Money eur = Money.of("100", "EUR");
        assertNotEquals(usd, eur);
    }

    @Test
    void equals_differentAmount_returnsFalse() {
        assertNotEquals(Money.of("100", "USD"), Money.of("101", "USD"));
    }

    @Test
    void constructor_nullAmount_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> new Money(null, "USD"));
    }

    @Test
    void constructor_nullCurrency_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void toString_returnsAmountAndCurrency() {
        String result = Money.of("42.5", "JPY").toString();
        assertTrue(result.contains("JPY"));
        assertTrue(result.contains("42.5"));
    }
}
