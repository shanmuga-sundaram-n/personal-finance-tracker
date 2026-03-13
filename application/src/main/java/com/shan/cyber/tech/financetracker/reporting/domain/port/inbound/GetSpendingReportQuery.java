package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.YearMonth;
import java.util.List;

public interface GetSpendingReportQuery {

    SpendingReportView getMonthlyReport(UserId userId, YearMonth month);

    List<MonthlyTrendItem> getTrend(UserId userId, int months);
}
