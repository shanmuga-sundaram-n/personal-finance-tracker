package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequestDto(
        @NotNull Long accountId,
        @NotNull Long categoryId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotBlank @Pattern(regexp = "^(INCOME|EXPENSE)$") String type,
        @NotNull LocalDate transactionDate,
        @Size(max = 500) String description,
        @Size(max = 200) String merchantName,
        @Size(max = 100) String referenceNumber) {
}
