package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

public record BudgetProgressSummary(
        Long budgetId,
        String categoryName,
        double percentUsed,
        String amount,
        String spent) {
}
