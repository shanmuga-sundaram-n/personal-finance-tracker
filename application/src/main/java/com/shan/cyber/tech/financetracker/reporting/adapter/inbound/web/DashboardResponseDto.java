package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.AccountBalanceSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.BudgetProgressSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.CategorySpendingSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.RecentTransactionSummary;

import java.util.List;

public record DashboardResponseDto(
        String netWorth,
        String totalAssets,
        String totalLiabilities,
        String currency,
        String currentMonthIncome,
        String currentMonthExpense,
        String netCashFlow,
        List<AccountBalanceSummary> accountBalances,
        List<CategorySpendingSummary> topExpenseCategories,
        List<RecentTransactionSummary> recentTransactions,
        List<BudgetProgressSummary> budgetAlerts) {
}
