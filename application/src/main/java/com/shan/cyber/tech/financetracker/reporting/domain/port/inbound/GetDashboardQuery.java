package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface GetDashboardQuery {
    DashboardView getDashboard(UserId userId);
}
