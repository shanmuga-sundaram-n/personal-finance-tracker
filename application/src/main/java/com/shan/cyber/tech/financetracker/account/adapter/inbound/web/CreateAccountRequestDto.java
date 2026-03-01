package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequestDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank String accountTypeCode,
        @NotNull BigDecimal initialBalance,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code") String currency,
        @Size(max = 100) String institutionName,
        @Pattern(regexp = "^\\d{4}$", message = "Must be exactly 4 digits") String accountNumberLast4) {
}
