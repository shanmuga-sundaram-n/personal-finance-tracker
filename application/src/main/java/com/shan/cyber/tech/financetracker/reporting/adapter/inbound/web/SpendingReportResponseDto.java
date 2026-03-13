package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.CategorySpendingSummary;

import java.util.List;

public record SpendingReportResponseDto(
        String month,
        String totalIncome,
        String totalExpense,
        String netFlow,
        List<CategorySpendingSummary> categoryBreakdown) {
}
