package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CopyBudgetsFromPreviousMonthRequestDto(
        @NotNull @Min(2000) @Max(2100) Integer targetYear,
        @NotNull @Min(1) @Max(12) Integer targetMonth,
        @NotNull Boolean overwriteExisting
) {}
