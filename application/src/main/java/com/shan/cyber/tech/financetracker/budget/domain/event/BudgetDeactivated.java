package com.shan.cyber.tech.financetracker.budget.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record BudgetDeactivated(
        BudgetId budgetId,
        UserId userId) implements DomainEvent {
}
