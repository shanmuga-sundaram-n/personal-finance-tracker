package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

import java.util.List;

public record SpendingReportView(
        String month,
        String totalIncome,
        String totalExpense,
        String netFlow,
        List<CategorySpendingSummary> categoryBreakdown) {
}
