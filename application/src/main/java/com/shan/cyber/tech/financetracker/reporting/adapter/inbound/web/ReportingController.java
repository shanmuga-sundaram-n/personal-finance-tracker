package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.DashboardView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetDashboardQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetSpendingReportQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.SpendingReportView;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Reporting", description = "Read-only financial summaries — dashboard, spending analysis, and monthly trends")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
        summary = "Get the financial dashboard",
        description = "Returns a snapshot of the user's financial position: net worth, account balances, current-month income/expense, top expense categories, recent transactions, and any active budget alerts."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard data",
            content = @Content(schema = @Schema(implementation = DashboardResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
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

    @Operation(
        summary = "Get the monthly spending report",
        description = "Returns income, expense, and net cash flow broken down by category for a given month. Defaults to the current month if no month is supplied."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Monthly spending report",
            content = @Content(schema = @Schema(implementation = SpendingReportResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid month format — expected yyyy-MM",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/spending")
    public SpendingReportResponseDto getSpendingReport(
            @Parameter(description = "Month in yyyy-MM format. Defaults to the current month.", example = "2025-03")
            @RequestParam(defaultValue = "") String month) {
        UserId userId = currentUserId();
        YearMonth yearMonth = month.isEmpty() ? YearMonth.now() : YearMonth.parse(month);
        SpendingReportView view = getSpendingReportQuery.getMonthlyReport(userId, yearMonth);
        return new SpendingReportResponseDto(
                view.month(), view.totalIncome(), view.totalExpense(),
                view.netFlow(), view.categoryBreakdown());
    }

    @Operation(
        summary = "Get monthly trend data",
        description = "Returns income, expense, and net cash flow aggregates for the most recent N months (newest first). Useful for rendering trend charts."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Monthly trend data",
            content = @Content(schema = @Schema(implementation = MonthlyTrendResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/trend")
    public MonthlyTrendResponseDto getTrend(
            @Parameter(description = "Number of months to include in the trend (default 6, max typically 24)", example = "6")
            @RequestParam(defaultValue = "6") int months) {
        UserId userId = currentUserId();
        List<MonthlyTrendItem> trend = getSpendingReportQuery.getTrend(userId, months);
        return new MonthlyTrendResponseDto(trend);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }
}
