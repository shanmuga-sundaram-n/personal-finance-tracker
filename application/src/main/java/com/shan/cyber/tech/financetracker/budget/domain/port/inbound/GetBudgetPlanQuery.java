package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import java.time.LocalDate;

public interface GetBudgetPlanQuery {
    BudgetPlanView getBudgetPlan(UserId userId, LocalDate startDate, LocalDate endDate);
}
