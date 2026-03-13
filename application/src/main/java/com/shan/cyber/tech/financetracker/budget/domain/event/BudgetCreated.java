package com.shan.cyber.tech.financetracker.budget.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record BudgetCreated(
        BudgetId budgetId,
        UserId userId,
        CategoryId categoryId,
        Money amount) implements DomainEvent {
}
