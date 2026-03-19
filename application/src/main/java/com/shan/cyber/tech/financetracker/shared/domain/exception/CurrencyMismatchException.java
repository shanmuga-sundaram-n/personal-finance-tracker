package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(String fromCurrency, String toCurrency) {
        super("CURRENCY_MISMATCH",
                "Cannot operate on different currencies: " + fromCurrency + " vs " + toCurrency);
    }
}
