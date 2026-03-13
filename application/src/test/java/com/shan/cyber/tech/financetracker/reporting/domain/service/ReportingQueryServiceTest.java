package com.shan.cyber.tech.financetracker.reporting.domain.service;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.DashboardView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.SpendingReportView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.AccountQueryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.BudgetSummaryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.NetWorthSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.TransactionSummaryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.UserPreferencesPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingQueryServiceTest {

    @Mock private AccountQueryPort accountQueryPort;
    @Mock private TransactionSummaryPort transactionSummaryPort;
    @Mock private BudgetSummaryPort budgetSummaryPort;
    @Mock private UserPreferencesPort userPreferencesPort;

    private ReportingQueryService service;

    private static final UserId USER = new UserId(1L);

    @BeforeEach
    void setUp() {
        service = new ReportingQueryService(accountQueryPort, transactionSummaryPort, budgetSummaryPort, userPreferencesPort);
    }

    @Test
    void getDashboard_returnsAggregatedView() {
        NetWorthSummary netWorth = new NetWorthSummary("5000.00", "1000.00", "4000.00", "EUR");
        when(accountQueryPort.getNetWorth(USER)).thenReturn(netWorth);
        when(userPreferencesPort.getPreferredCurrency(USER)).thenReturn("EUR");
        when(transactionSummaryPort.sumByType(eq(USER), eq("INCOME"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionSummaryPort.sumByType(eq(USER), eq("EXPENSE"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1500.00"));
        when(accountQueryPort.getAccountBalances(USER)).thenReturn(List.of());
        when(transactionSummaryPort.topExpenseCategories(eq(USER), any(), any(), anyInt())).thenReturn(List.of());
        when(transactionSummaryPort.recentTransactions(eq(USER), anyInt())).thenReturn(List.of());
        when(budgetSummaryPort.getActiveBudgetAlerts(USER)).thenReturn(List.of());

        DashboardView result = service.getDashboard(USER);

        assertNotNull(result);
        assertEquals("4000.00", result.netWorth());
        assertEquals("EUR", result.currency());
        assertEquals("2000.00", result.currentMonthIncome());
        assertEquals("1500.00", result.currentMonthExpense());
        assertEquals("500.00", result.netCashFlow());
    }

    @Test
    void getMonthlyReport_returnsSpendingBreakdown() {
        YearMonth month = YearMonth.of(2025, 3);
        when(transactionSummaryPort.sumByType(eq(USER), eq("INCOME"), any(), any()))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionSummaryPort.sumByType(eq(USER), eq("EXPENSE"), any(), any()))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionSummaryPort.topExpenseCategories(eq(USER), any(), any(), eq(20))).thenReturn(List.of());

        SpendingReportView result = service.getMonthlyReport(USER, month);

        assertNotNull(result);
        assertEquals("2025-03", result.month());
        assertEquals("3000.00", result.totalIncome());
        assertEquals("2000.00", result.totalExpense());
        assertEquals("1000.00", result.netFlow());
        assertNotNull(result.categoryBreakdown());
    }

    @Test
    void getTrend_returnsCorrectNumberOfMonthlyItems() {
        when(transactionSummaryPort.sumByType(any(), anyString(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        List<MonthlyTrendItem> result = service.getTrend(USER, 6);

        assertEquals(6, result.size());
    }

    @Test
    void getTrend_monthsAreInChronologicalOrder() {
        when(transactionSummaryPort.sumByType(any(), anyString(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        List<MonthlyTrendItem> trend = service.getTrend(USER, 3);

        // Oldest month first
        assertTrue(trend.get(0).month().compareTo(trend.get(1).month()) < 0);
        assertTrue(trend.get(1).month().compareTo(trend.get(2).month()) < 0);
    }

    @Test
    void getDashboard_usesPreferredCurrencyFromUserPreferencesPort() {
        when(accountQueryPort.getNetWorth(USER)).thenReturn(new NetWorthSummary("0", "0", "0", "USD"));
        when(userPreferencesPort.getPreferredCurrency(USER)).thenReturn("GBP");
        when(transactionSummaryPort.sumByType(any(), anyString(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(accountQueryPort.getAccountBalances(USER)).thenReturn(List.of());
        when(transactionSummaryPort.topExpenseCategories(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(transactionSummaryPort.recentTransactions(any(), anyInt())).thenReturn(List.of());
        when(budgetSummaryPort.getActiveBudgetAlerts(USER)).thenReturn(List.of());

        DashboardView result = service.getDashboard(USER);

        assertEquals("GBP", result.currency());
    }
}
