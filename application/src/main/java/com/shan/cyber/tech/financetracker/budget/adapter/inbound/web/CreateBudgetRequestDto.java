package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequestDto(
        @NotNull Long categoryId,
        @NotBlank String periodType,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        boolean rolloverEnabled,
        @Min(1) @Max(100) Integer alertThresholdPct) {
}
