package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertBudgetByCategoryRequestDto(
    @NotNull Long categoryId,
    @NotBlank @Pattern(regexp = "^(WEEKLY|BI_WEEKLY|MONTHLY|QUARTERLY|SEMI_ANNUAL|ANNUALLY|CUSTOM)$",
                       message = "periodType must be one of: WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUALLY, CUSTOM")
    String periodType,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
