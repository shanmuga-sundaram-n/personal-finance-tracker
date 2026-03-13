package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

import java.util.List;

public record DashboardView(
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
