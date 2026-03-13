package com.shan.cyber.tech.financetracker.reporting.domain.port.outbound;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.BudgetProgressSummary;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface BudgetSummaryPort {

    List<BudgetProgressSummary> getActiveBudgetAlerts(UserId userId);
}
