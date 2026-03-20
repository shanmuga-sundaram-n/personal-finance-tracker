package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BudgetResponseDto(
        Long id,
        Long categoryId,
        String categoryName,
        String periodType,
        String amount,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        boolean rolloverEnabled,
        Integer alertThresholdPct,
        boolean isActive,
        String spentAmount,
        String remainingAmount,
        double percentUsed,
        boolean alertTriggered,
        String rolloverAmountAdded,
        String effectiveBudgetThisPeriod,
        OffsetDateTime createdAt) {
}
