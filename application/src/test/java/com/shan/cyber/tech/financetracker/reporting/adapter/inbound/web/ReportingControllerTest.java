package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.DashboardView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetDashboardQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.GetSpendingReportQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.SpendingReportView;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportingController.class)
@Import(GlobalExceptionHandler.class)
class ReportingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private GetDashboardQuery getDashboardQuery;
    @MockBean private GetSpendingReportQuery getSpendingReportQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final DashboardView SAMPLE_DASHBOARD = new DashboardView(
            "4000.00", "5000.00", "1000.00", "USD",
            "2000.00", "1500.00", "500.00",
            List.of(), List.of(), List.of(), List.of());
    private static final SpendingReportView SAMPLE_SPENDING = new SpendingReportView(
            "2025-03", "3000.00", "2000.00", "1000.00", List.of());

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void getDashboard_returns200WithDashboardData() throws Exception {
        when(getDashboardQuery.getDashboard(any())).thenReturn(SAMPLE_DASHBOARD);

        mockMvc.perform(get("/api/v1/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value("4000.00"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.currentMonthIncome").value("2000.00"))
                .andExpect(jsonPath("$.netCashFlow").value("500.00"));
    }

    @Test
    void getSpendingReport_noParam_usesCurrentMonth() throws Exception {
        when(getSpendingReportQuery.getMonthlyReport(any(), any())).thenReturn(SAMPLE_SPENDING);

        mockMvc.perform(get("/api/v1/reports/spending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2025-03"))
                .andExpect(jsonPath("$.totalIncome").value("3000.00"))
                .andExpect(jsonPath("$.totalExpense").value("2000.00"));
    }

    @Test
    void getSpendingReport_withMonthParam_passesMonthToService() throws Exception {
        when(getSpendingReportQuery.getMonthlyReport(any(), any())).thenReturn(SAMPLE_SPENDING);

        mockMvc.perform(get("/api/v1/reports/spending").param("month", "2025-03"))
                .andExpect(status().isOk());
    }

    @Test
    void getTrend_defaultMonths_returns200WithTrendData() throws Exception {
        List<MonthlyTrendItem> trend = List.of(
                new MonthlyTrendItem("2025-01", "1000", "800", "200"),
                new MonthlyTrendItem("2025-02", "1200", "900", "300"),
                new MonthlyTrendItem("2025-03", "1100", "950", "150"));
        when(getSpendingReportQuery.getTrend(any(), anyInt())).thenReturn(trend);

        mockMvc.perform(get("/api/v1/reports/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].month").value("2025-01"))
                .andExpect(jsonPath("$.months").isArray());
    }

    @Test
    void getTrend_customMonthsParam_returns200() throws Exception {
        when(getSpendingReportQuery.getTrend(any(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/trend").param("months", "12"))
                .andExpect(status().isOk());
    }
}
