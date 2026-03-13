package com.shan.cyber.tech.financetracker.reporting.domain.service;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.CategorySpendingSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.DashboardView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetDashboardQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetSpendingReportQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.SpendingReportView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.AccountQueryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.BudgetSummaryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.NetWorthSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.TransactionSummaryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportingQueryService implements GetDashboardQuery, GetSpendingReportQuery {

    private final AccountQueryPort accountQueryPort;
    private final TransactionSummaryPort transactionSummaryPort;
    private final BudgetSummaryPort budgetSummaryPort;

    public ReportingQueryService(AccountQueryPort accountQueryPort,
                                  TransactionSummaryPort transactionSummaryPort,
                                  BudgetSummaryPort budgetSummaryPort) {
        this.accountQueryPort = accountQueryPort;
        this.transactionSummaryPort = transactionSummaryPort;
        this.budgetSummaryPort = budgetSummaryPort;
    }

    @Override
    public DashboardView getDashboard(UserId userId) {
        NetWorthSummary netWorth = accountQueryPort.getNetWorth(userId);

        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        BigDecimal income = transactionSummaryPort.sumByType(userId, "INCOME", monthStart, monthEnd);
        BigDecimal expense = transactionSummaryPort.sumByType(userId, "EXPENSE", monthStart, monthEnd);
        BigDecimal netCashFlow = income.subtract(expense);

        return new DashboardView(
                netWorth.netWorth(),
                netWorth.totalAssets(),
                netWorth.totalLiabilities(),
                netWorth.currency(),
                income.toPlainString(),
                expense.toPlainString(),
                netCashFlow.toPlainString(),
                accountQueryPort.getAccountBalances(userId),
                transactionSummaryPort.topExpenseCategories(userId, monthStart, monthEnd, 5),
                transactionSummaryPort.recentTransactions(userId, 5),
                budgetSummaryPort.getActiveBudgetAlerts(userId));
    }

    @Override
    public SpendingReportView getMonthlyReport(UserId userId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        BigDecimal income = transactionSummaryPort.sumByType(userId, "INCOME", from, to);
        BigDecimal expense = transactionSummaryPort.sumByType(userId, "EXPENSE", from, to);
        BigDecimal netFlow = income.subtract(expense);

        List<CategorySpendingSummary> breakdown = transactionSummaryPort.topExpenseCategories(
                userId, from, to, 20);

        return new SpendingReportView(
                month.toString(),
                income.toPlainString(),
                expense.toPlainString(),
                netFlow.toPlainString(),
                breakdown);
    }

    @Override
    public List<MonthlyTrendItem> getTrend(UserId userId, int months) {
        List<MonthlyTrendItem> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate from = month.atDay(1);
            LocalDate to = month.atEndOfMonth();

            BigDecimal income = transactionSummaryPort.sumByType(userId, "INCOME", from, to);
            BigDecimal expense = transactionSummaryPort.sumByType(userId, "EXPENSE", from, to);
            BigDecimal net = income.subtract(expense);

            trend.add(new MonthlyTrendItem(
                    month.toString(),
                    income.toPlainString(),
                    expense.toPlainString(),
                    net.toPlainString()));
        }

        return trend;
    }
}
