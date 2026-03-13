package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;

public record UpdateBudgetCommand(
        BudgetId budgetId,
        UserId userId,
        Money amount,
        LocalDate endDate,
        boolean rolloverEnabled,
        Integer alertThresholdPct) {
}
