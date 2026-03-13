package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface DeactivateBudgetUseCase {

    void deactivateBudget(BudgetId budgetId, UserId userId);
}
