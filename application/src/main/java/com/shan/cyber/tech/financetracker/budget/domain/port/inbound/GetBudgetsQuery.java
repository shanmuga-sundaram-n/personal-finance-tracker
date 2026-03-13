package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface GetBudgetsQuery {

    List<BudgetView> getActiveByUser(UserId userId);

    BudgetView getById(BudgetId budgetId, UserId userId);
}
