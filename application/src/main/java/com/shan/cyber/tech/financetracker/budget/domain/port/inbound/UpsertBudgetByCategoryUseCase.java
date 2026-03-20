package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;

public interface UpsertBudgetByCategoryUseCase {
    BudgetId upsertBudget(UpsertBudgetByCategoryCommand command);
}
