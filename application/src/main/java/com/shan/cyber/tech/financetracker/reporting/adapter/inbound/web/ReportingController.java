package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.DashboardView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetDashboardQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetSpendingReportQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.SpendingReportView;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final GetDashboardQuery getDashboardQuery;
    private final GetSpendingReportQuery getSpendingReportQuery;

    public ReportingController(GetDashboardQuery getDashboardQuery,
                                GetSpendingReportQuery getSpendingReportQuery) {
        this.getDashboardQuery = getDashboardQuery;
        this.getSpendingReportQuery = getSpendingReportQuery;
    }

    @GetMapping("/dashboard")
    public DashboardResponseDto getDashboard() {
        UserId userId = currentUserId();
        DashboardView view = getDashboardQuery.getDashboard(userId);
        return new DashboardResponseDto(
                view.netWorth(), view.totalAssets(), view.totalLiabilities(), view.currency(),
                view.currentMonthIncome(), view.currentMonthExpense(), view.netCashFlow(),
                view.accountBalances(), view.topExpenseCategories(),
                view.recentTransactions(), view.budgetAlerts());
    }

    @GetMapping("/spending")
    public SpendingReportResponseDto getSpendingReport(
            @RequestParam(defaultValue = "") String month) {
        UserId userId = currentUserId();
        YearMonth yearMonth = month.isEmpty() ? YearMonth.now() : YearMonth.parse(month);
        SpendingReportView view = getSpendingReportQuery.getMonthlyReport(userId, yearMonth);
        return new SpendingReportResponseDto(
                view.month(), view.totalIncome(), view.totalExpense(),
                view.netFlow(), view.categoryBreakdown());
    }

    @GetMapping("/trend")
    public MonthlyTrendResponseDto getTrend(
            @RequestParam(defaultValue = "6") int months) {
        UserId userId = currentUserId();
        List<MonthlyTrendItem> trend = getSpendingReportQuery.getTrend(userId, months);
        return new MonthlyTrendResponseDto(trend);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }
}
